package github.couryrr.backend.playbook.data.service;

import github.couryrr.backend.playbook.data.gen.user.v1.*;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserServiceIntegrationTest {

    @Autowired
    private GrpcChannelFactory channelFactory;

    private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

    @BeforeEach
    void setUp() {
        ManagedChannel channel = channelFactory.createChannel("test");
        userServiceStub = UserServiceGrpc.newBlockingStub(channel);
    }

    @Test
    void shouldCreateUserSuccessfully() {
        CreateUserRequest request = CreateUserRequest.newBuilder()
                .setUsername("testuser")
                .setEmail("test@example.com")
                .setPassword("password123")
                .setFirstName("Test")
                .setLastName("User")
                .build();

        CreateUserResponse response = userServiceStub.createUser(request);

        assertThat(response.hasUser()).isTrue();
        assertThat(response.getUser().getUsername()).isEqualTo("testuser");
        assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");
        assertThat(response.getUser().getFirstName()).isEqualTo("Test");
        assertThat(response.getUser().getLastName()).isEqualTo("User");
        assertThat(response.getUser().getId()).isNotEmpty();
        assertThat(response.getUser().hasCreatedAt()).isTrue();
        assertThat(response.getUser().hasUpdatedAt()).isTrue();
    }

    @Test
    void shouldReturnValidationErrorForInvalidCreateUserRequest() {
        CreateUserRequest request = CreateUserRequest.newBuilder()
                .setUsername("")
                .setEmail("invalid-email")
                .setPassword("123")
                .setFirstName("")
                .setLastName("")
                .build();

        CreateUserResponse response = userServiceStub.createUser(request);

        assertThat(response.hasError()).isTrue();
        assertThat(response.getError().getCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getError().getFieldErrorsList()).hasSize(5);
    }

    @Test
    void shouldValidateEmailCorrectly() {
        ValidateEmailRequest request = ValidateEmailRequest.newBuilder()
                .setEmail("valid@example.com")
                .build();

        ValidateEmailResponse response = userServiceStub.validateEmail(request);

        assertThat(response.getIsValid()).isTrue();
        assertThat(response.getIsAvailable()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Email is valid and available");
    }

    @Test
    void shouldReturnEmailNotAvailableAfterUserCreation() {
        String email = "taken@example.com";
        
        CreateUserRequest createRequest = CreateUserRequest.newBuilder()
                .setUsername("testuser")
                .setEmail(email)
                .setPassword("password123")
                .setFirstName("Test")
                .setLastName("User")
                .build();
        userServiceStub.createUser(createRequest);

        ValidateEmailRequest validateRequest = ValidateEmailRequest.newBuilder()
                .setEmail(email)
                .build();

        ValidateEmailResponse response = userServiceStub.validateEmail(validateRequest);

        assertThat(response.getIsValid()).isTrue();
        assertThat(response.getIsAvailable()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Email is already taken");
    }

    @Test
    void shouldCheckUsernameAvailability() {
        CheckUsernameRequest request = CheckUsernameRequest.newBuilder()
                .setUsername("availableuser")
                .build();

        CheckUsernameResponse response = userServiceStub.checkUsername(request);

        assertThat(response.getIsAvailable()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Username is available");
    }

    @Test
    void shouldReturnUsernameNotAvailableAfterUserCreation() {
        String username = "takenuser";
        
        CreateUserRequest createRequest = CreateUserRequest.newBuilder()
                .setUsername(username)
                .setEmail("test@example.com")
                .setPassword("password123")
                .setFirstName("Test")
                .setLastName("User")
                .build();
        userServiceStub.createUser(createRequest);

        CheckUsernameRequest checkRequest = CheckUsernameRequest.newBuilder()
                .setUsername(username)
                .build();

        CheckUsernameResponse response = userServiceStub.checkUsername(checkRequest);

        assertThat(response.getIsAvailable()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Username is already taken");
    }

    @Test
    void shouldGetUserById() {
        CreateUserRequest createRequest = CreateUserRequest.newBuilder()
                .setUsername("getuser")
                .setEmail("getuser@example.com")
                .setPassword("password123")
                .setFirstName("Get")
                .setLastName("User")
                .build();
        CreateUserResponse createResponse = userServiceStub.createUser(createRequest);
        String userId = createResponse.getUser().getId();

        GetUserRequest getRequest = GetUserRequest.newBuilder()
                .setId(userId)
                .build();

        GetUserResponse response = userServiceStub.getUser(getRequest);

        assertThat(response.hasUser()).isTrue();
        assertThat(response.getUser().getId()).isEqualTo(userId);
        assertThat(response.getUser().getUsername()).isEqualTo("getuser");
        assertThat(response.getUser().getEmail()).isEqualTo("getuser@example.com");
    }

    @Test
    void shouldReturnNotFoundForNonExistentUser() {
        GetUserRequest request = GetUserRequest.newBuilder()
                .setId("non-existent-id")
                .build();

        GetUserResponse response = userServiceStub.getUser(request);

        assertThat(response.hasError()).isTrue();
        assertThat(response.getError().getMessage()).contains("User not found");
    }

    @Test
    void shouldUpdateUserSuccessfully() {
        CreateUserRequest createRequest = CreateUserRequest.newBuilder()
                .setUsername("updateuser")
                .setEmail("update@example.com")
                .setPassword("password123")
                .setFirstName("Update")
                .setLastName("User")
                .build();
        CreateUserResponse createResponse = userServiceStub.createUser(createRequest);
        String userId = createResponse.getUser().getId();

        UpdateUserRequest updateRequest = UpdateUserRequest.newBuilder()
                .setId(userId)
                .setFirstName("Updated")
                .setLastName("Name")
                .build();

        UpdateUserResponse response = userServiceStub.updateUser(updateRequest);

        assertThat(response.hasUser()).isTrue();
        assertThat(response.getUser().getFirstName()).isEqualTo("Updated");
        assertThat(response.getUser().getLastName()).isEqualTo("Name");
        assertThat(response.getUser().getUsername()).isEqualTo("updateuser");
        assertThat(response.getUser().getEmail()).isEqualTo("update@example.com");
    }

    @Test
    void shouldAuthenticateUserSuccessfully() {
        String email = "auth@example.com";
        String password = "password123";
        
        CreateUserRequest createRequest = CreateUserRequest.newBuilder()
                .setUsername("authuser")
                .setEmail(email)
                .setPassword(password)
                .setFirstName("Auth")
                .setLastName("User")
                .build();
        userServiceStub.createUser(createRequest);

        AuthenticateUserRequest authRequest = AuthenticateUserRequest.newBuilder()
                .setEmail(email)
                .setPassword(password)
                .build();

        AuthenticateUserResponse response = userServiceStub.authenticateUser(authRequest);

        assertThat(response.hasUser()).isTrue();
        assertThat(response.getUser().getEmail()).isEqualTo(email);
        assertThat(response.getUser().getUsername()).isEqualTo("authuser");
    }

    @Test
    void shouldReturnAuthenticationErrorForInvalidCredentials() {
        AuthenticateUserRequest request = AuthenticateUserRequest.newBuilder()
                .setEmail("nonexistent@example.com")
                .setPassword("wrongpassword")
                .build();

        AuthenticateUserResponse response = userServiceStub.authenticateUser(request);

        assertThat(response.hasError()).isTrue();
        assertThat(response.getError().getCode()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(response.getError().getMessage()).isEqualTo("Invalid email or password");
    }
}
