# Skill: Java-Testing

## Zweck
Aktiviere diesen Skill beim Schreiben, Prüfen oder Refactoring
von Tests.

## Stack
- **Spring Boot Test** – Integrationstests
- **Testcontainers** – echte DB in Integrationstests

## Testpyramide
```
      /\      E2E / @SpringBootTest (wenige)
     /--\     Integration / @DataJpaTest, @WebMvcTest
    /----\    Unit-Tests mit Mockito (viele)
```

## Prinzipien
- **AAA-Pattern**: Arrange, Act, Assert
- **Ein Test = ein Verhalten**
- Sprechende Namen: `shouldThrowException_whenUserNotFound`
- Deterministisch & isoliert
- AssertJ statt JUnit-Assertions (`assertThat(...)`)
- **F.I.R.S.T.**: Fast, Independent, Repeatable, Self-validating, Timely

## Unit-Test Beispiel (Mockito + AssertJ)
```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldReturnUser_whenUserExists() {
        // Arrange
        User user = new User(1L, "Anna");
        UserDto dto = new UserDto(1L, "Anna");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(dto);

        // Act
        UserDto result = userService.findById(1L);

        // Assert
        assertThat(result.name()).isEqualTo("Anna");
        verify(userRepository).findById(1L);
    }

    @Test
    void shouldThrowException_whenUserNotFound() {
        // Arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(UserNotFoundException.class);
    }
}
```

## Parametrisierte Tests
```java
@ParameterizedTest
@ValueSource(strings = {"", "  ", "\t"})
void shouldThrowException_whenNameIsBlank(String invalidName) {
    assertThatThrownBy(() -> userService.create(invalidName))
            .isInstanceOf(ValidationException.class);
}
```

## Slice-Tests (Spring Boot)
```java
// Nur Web-Layer testen
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    UserService userService;

    @Test
    void shouldReturn200_whenUserExists() throws Exception {
        when(userService.findById(1L))
            .thenReturn(new UserDto(1L, "Anna"));

        mockMvc.perform(get("/api/users/1"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.name").value("Anna"));
    }
}

// Nur JPA-Layer testen
@DataJpaTest
class UserRepositoryTest {

    @Autowired
    UserRepository userRepository;

    @Test
    void shouldFindUserByEmail() {
        userRepository.save(new User(null, "Anna", "anna@example.com"));

        Optional<User> result = userRepository.findByEmail("anna@example.com");

        assertThat(result).isPresent();
    }
}
```

## Integrationstest mit Testcontainers
```java
@SpringBootTest
@Testcontainers
class UserIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    UserService userService;

    @Test
    void shouldPersistAndRetrieveUser() {
        UserDto created = userService.create(
            new CreateUserRequest("Anna", "anna@example.com"));

        UserDto found = userService.findById(created.id());

        assertThat(found.name()).isEqualTo("Anna");
    }
}
```

## Checkliste
1. Happy-Path getestet?
2. Fehlerfälle & Edge-Cases (null, leer, ungültig)?
3. Wurde gemockt, was extern ist (Repo, API)?
4. AssertJ für lesbare Assertions genutzt?
5. `gradle test` grün?
6. Keine flaky Tests (Zeit/Reihenfolge)?

## Mocking-Regeln
- Externe Abhängigkeiten mocken (Repository, RestClient)
- Interne Logik NICHT mocken
- `verify(...)` nur für relevante Interaktionen
- `@MockBean` in Spring-Slice-Tests, `@Mock` in reinen Unit-Tests

## Nützliche Befehle
- Alle Tests: `gradle test`
- Einzelne Klasse: `gradle test -Dtest=UserServiceTest`
- Einzelne Methode: `gradle test -Dtest=UserServiceTest#shouldReturnUser_whenUserExists`
- Mit Coverage (JaCoCo): `gradle test jacoco:report`