package io.github.keymaster65.helloai.adapter.out.gitdata;

import io.github.keymaster65.helloai.domain.model.Difficulty;
import io.github.keymaster65.helloai.domain.model.Ingredient;
import io.github.keymaster65.helloai.domain.model.PreparationStep;
import io.github.keymaster65.helloai.domain.model.Recipe;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Reads the recipes of the Liquibase seed changeset from the classpath (ADR 0055).
 *
 * <p>The initial population of both stores comes from <em>one</em> file. Copying the six recipes
 * into a second resource would mean two versions of the same data, and „identical" would be a claim
 * instead of a property. The price is that this adapter reads a changelog of the other one &ndash;
 * as a resource, not as a Liquibase concept: what is used here is XML, not migration.
 *
 * <p>The children resolve their recipe the way the changeset writes it: through the title in the
 * sub-select of {@code recipe_id}. That is the only link the file has, because the identifiers are
 * {@code GENERATED ALWAYS} and therefore not in it.
 */
final class SeedChangelog {

    /** The seed changeset of the relational adapter &ndash; the single source of both populations. */
    static final String RESOURCE = "db/changelog/changes/0002-seed-fasting-recipes.xml";

    /** {@code (SELECT "id" FROM "recipe" WHERE "title" = 'Fastentee-Mischung')} &ndash; the title. */
    private static final Pattern TITLE_IN_SUBSELECT = Pattern.compile("'(.*)'", Pattern.DOTALL);

    private SeedChangelog() {
    }

    /**
     * @return the seeded recipes in the order of the changeset, each with its ingredients and steps
     * @throws GitDataException if the resource is missing or cannot be read as expected
     */
    static List<Recipe> recipes() {
        try (InputStream source = SeedChangelog.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (source == null) {
                throw new GitDataException("the seed changeset is not on the classpath");
            }
            return parse(source);
        } catch (IOException | ParserConfigurationException | SAXException cause) {
            throw new GitDataException("cannot read the seed changeset", cause);
        }
    }

    private static List<Recipe> parse(InputStream source)
            throws IOException, ParserConfigurationException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // The changelog names an XSD; without this the parser would fetch it over the network and
        // the start of the application would hang on the reachability of liquibase.org.
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setNamespaceAware(false);
        factory.setValidating(false);

        NodeList inserts = factory.newDocumentBuilder()
                .parse(source)
                .getElementsByTagName("insert");

        // Insertion order is the order of the changeset, and that is the order the recipes get
        // their identifiers in – in both stores.
        Map<String, List<Ingredient>> ingredients = new LinkedHashMap<>();
        Map<String, List<PreparationStep>> steps = new LinkedHashMap<>();
        List<Element> recipes = new ArrayList<>();

        for (int index = 0; index < inserts.getLength(); index++) {
            Element insert = (Element) inserts.item(index);
            switch (insert.getAttribute("tableName")) {
                case "recipe" -> recipes.add(insert);
                case "ingredient" -> ingredients
                        .computeIfAbsent(recipeOf(insert), _ -> new ArrayList<>())
                        .add(ingredientOf(insert));
                case "preparation_step" -> steps
                        .computeIfAbsent(recipeOf(insert), _ -> new ArrayList<>())
                        .add(stepOf(insert));
                default -> throw new GitDataException(
                        "the seed changeset inserts into an unknown table: "
                                + insert.getAttribute("tableName"));
            }
        }

        return recipes.stream().map(insert -> {
            String title = value(insert, "title");
            return Recipe.curried()
                    .id(null)
                    .title(title)
                    .description(value(insert, "description"))
                    .servings(number(insert, "servings"))
                    .prepTimeMinutes(number(insert, "prep_time_minutes"))
                    .difficulty(Difficulty.valueOf(value(insert, "difficulty")))
                    .ingredients(ingredients.getOrDefault(title, List.of()))
                    .steps(steps.getOrDefault(title, List.of()));
        }).toList();
    }

    private static Ingredient ingredientOf(Element insert) {
        BigDecimal quantity = decimal(insert, "quantity");
        return Ingredient.curried()
                .name(value(insert, "name"))
                .quantity(quantity)
                .unit(value(insert, "unit"));
    }

    private static PreparationStep stepOf(Element insert) {
        Integer position = number(insert, "position");
        if (position == null) {
            throw new GitDataException("a preparation step of the seed changeset has no position");
        }
        return new PreparationStep(position, value(insert, "instruction"));
    }

    /** The title of the recipe a child row belongs to, taken from its {@code recipe_id} sub-select. */
    private static String recipeOf(Element insert) {
        Element column = column(insert, "recipe_id");
        if (column == null) {
            throw new GitDataException("a child row of the seed changeset has no recipe_id");
        }
        Matcher title = TITLE_IN_SUBSELECT.matcher(column.getAttribute("valueComputed"));
        if (!title.find()) {
            throw new GitDataException("the recipe_id of the seed changeset names no title");
        }
        return title.group(1);
    }

    private static String value(Element insert, String columnName) {
        Element column = column(insert, columnName);
        return column == null ? null : column.getAttribute("value");
    }

    private static Integer number(Element insert, String columnName) {
        Element column = column(insert, columnName);
        return column == null ? null : Integer.valueOf(column.getAttribute("valueNumeric"));
    }

    private static BigDecimal decimal(Element insert, String columnName) {
        Element column = column(insert, columnName);
        return column == null ? null : new BigDecimal(column.getAttribute("valueNumeric"));
    }

    private static Element column(Element insert, String columnName) {
        NodeList columns = insert.getElementsByTagName("column");
        for (int index = 0; index < columns.getLength(); index++) {
            Node column = columns.item(index);
            if (column instanceof Element element && columnName.equals(element.getAttribute("name"))) {
                return element;
            }
        }
        return null;
    }
}
