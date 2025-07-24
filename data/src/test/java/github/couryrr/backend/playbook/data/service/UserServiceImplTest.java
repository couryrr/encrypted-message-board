package github.couryrr.backend.playbook.data.service;

import github.couryrr.backend.playbook.data.gen.user.v1.*;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private UserServiceImpl userService;

    @Mock
    private StreamObserver<CreateUserResponse> createUserResponseObserver;

    @Mock
    private StreamObserver<GetUserResponse> getUserResponseObserver;

    @Mock
    private StreamObserver<UpdateUserResponse> updateUserResponseObserver;

    @Mock
    private StreamObserver<ValidateEmailResponse> validateEmailResponseObserver;

    @Mock
    private StreamObserver<CheckUsernameResponse> checkUsernameResponseObserver;

    @Mock
    private StreamObserver<AuthenticateUserResponse> authenticateUserResponseObserver;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl();
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

        userService.createUser(request, createUserResponseObserver);

        ArgumentCaptor<CreateUserResponse> responseCaptor = ArgumentCaptor.forClass(CreateUserResponse.class);
        verify(createUserResponseObserver).onNext(responseCaptor.capture());
        verify(createUserResponseObserver).onCompleted();

        CreateUserResponse response = responseCaptor.getValue();
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
    void shouldReturnValidationErrorForEmptyUsername() {
        CreateUserRequest request = CreateUserRequest.newBuilder()
                .setUsername("")
                .setEmail("test@example.com")
                .setPassword("password123")
                .setFirstName("Test")
                .setLastName("User")
                .build();

        userService.createUser(request, createUserResponseObserver);

        ArgumentCaptor<CreateUserResponse> responseCaptor = ArgumentCaptor.forClass(CreateUserResponse.class);
        verify(createUserResponseObserver).onNext(responseCaptor.capture());
        verify(createUserResponseObserver).onCompleted();

        CreateUserResponse response = responseCaptor.getValue();
        assertThat(response.hasError()).isTrue();
        assertThat(response.getError().getCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getError().getFieldErrorsList())
                .anyMatch(error -> error.getField().equals("username") && 
                         error.getMessage().equals("Username is required"));
    }

    @Test
    void shouldReturnValidationErrorForInvalidEmail() {
        CreateUserRequest request = CreateUserRequest.newBuilder()
                .setUsername("testuser")
                .setEmail("invalid-email")
                .setPassword("password123")
                .setFirstName("Test")
                .setLastName("User")
                .build();

        userService.createUser(request, createUserResponseObserver);

        ArgumentCaptor<CreateUserResponse> responseCaptor = ArgumentCaptor.forClass(CreateUserResponse.class);
        verify(createUserResponseObserver).onNext(responseCaptor.capture());
        verify(createUserResponseObserver).onCompleted();

        CreateUserResponse response = responseCaptor.getValue();
        assertThat(response.hasError()).isTrue();
        assertThat(response.getError().getFieldErrorsList())
                .anyMatch(error -> error.getField().equals("email") && 
                         error.getMessage().equals("Invalid email format"));
    }

    @Test
    void shouldReturnValidationErrorForShortPassword() {
        CreateUserRequest request = CreateUserRequest.newBuilder()
                .setUsername("testuser")
                .setEmail("test@example.com")
                .setPassword("123")
                .setFirstName("Test")
                .setLastName("User")
                .build();

        userService.createUser(request, createUserResponseObserver);

        ArgumentCaptor<CreateUserResponse> responseCaptor = ArgumentCaptor.forClass(CreateUserResponse.class);
        verify(createUserResponseObserver).onNext(responseCaptor.capture());
        verify(createUserResponseObserver).onCompleted();

        CreateUserResponse response = responseCaptor.getValue();
        assertThat(response.hasError()).isTrue();
        assertThat(response.getError().getFieldErrorsList())
                .anyMatch(error -> error.getField().equals("password") && 
                         error.getMessage().equals("Password must be at least 6 characters"));
    }

    @Test
    void shouldPreventDuplicateUsernames() {
        CreateUserRequest firstRequest = CreateUserRequest.newBuilder()
                .setUsername("duplicateuser")
                .setEmail("first@example.com")
                .setPassword("password123")
                .setFirstName("First")
                .setLastName("User")
                .build();

        CreateUserRequest secondRequest = CreateUserRequest.newBuilder()
                .setUsername("duplicateuser")
                .setEmail("second@example.com")
                .setPassword("password123")
                .setFirstName("Second")
                .setLastName("User")
                .build();

        userService.createUser(firstRequest, createUserResponseObserver);
        userService.createUser(secondRequest, createUserResponseObserver);

        ArgumentCaptor<CreateUserResponse> responseCaptor = ArgumentCaptor.forClass(CreateUserResponse.class);
        verify(createUserResponseObserver, times(2)).onNext(responseCaptor.capture());
        verify(createUserResponseObserver, times(2)).onCompleted();

        CreateUserResponse secondResponse = responseCaptor.getAllValues().get(1);
        assertThat(secondResponse.hasError()).isTrue();
        assertThat(secondResponse.getError().getFieldErrorsList())
                .anyMatch(error -> error.getField().equals("username") && 
                         error.getMessage().equals("Username is already taken"));
    }

    @Test
    void shouldPreventDuplicateEmails() {
        CreateUserRequest firstRequest = CreateUserRequest.newBuilder()
                .setUsername("firstuser")
                .setEmail("duplicate@example.com")
                .setPassword("password123")
                .setFirstName("First")
                .setLastName("User")
                .build();

        CreateUserRequest secondRequest = CreateUserRequest.newBuilder()
                .setUsername("seconduser")
                .setEmail("duplicate@example.com")
                .setPassword("password123")
                .setFirstName("Second")
                .setLastName("User")
                .build();

        userService.createUser(firstRequest, createUserResponseObserver);
        userService.createUser(secondRequest, createUserResponseObserver);

        ArgumentCaptor<CreateUserResponse> responseCaptor = ArgumentCaptor.forClass(CreateUserResponse.class);
        verify(createUserResponseObserver, times(2)).onNext(responseCaptor.capture());
        verify(createUserResponseObserver, times(2)).onCompleted();

        CreateUserResponse secondResponse = responseCaptor.getAllValues().get(1);
        assertThat(secondResponse.hasError()).isTrue();
        assertThat(secondResponse.getError().getFieldErrorsList())
                .anyMatch(error -> error.getField().equals("email") && 
                         error.getMessage().equals("Email is already taken"));
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

        userService.createUser(createRequest, createUserResponseObserver);

        ArgumentCaptor<CreateUserResponse> createResponseCaptor = ArgumentCaptor.forClass(CreateUserResponse.class);
        verify(createUserResponseObserver).onNext(createResponseCaptor.capture());
        String userId = createResponseCaptor.getValue().getUser().getId();

        GetUserRequest getRequest = GetUserRequest.newBuilder()
                .setId(userId)
                .build();

        userService.getUser(getRequest, getUserResponseObserver);

        ArgumentCaptor<GetUserResponse> getResponseCaptor = ArgumentCaptor.forClass(GetUserResponse.class);
        verify(getUserResponseObserver).onNext(getResponseCaptor.capture());
        verify(getUserResponseObserver).onCompleted();

        GetUserResponse response = getResponseCaptor.getValue();
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

        userService.getUser(request, getUserResponseObserver);

        ArgumentCaptor<GetUserResponse> responseCaptor = ArgumentCaptor.forClass(GetUserResponse.class);
        verify(getUserResponseObserver).onNext(responseCaptor.capture());
        verify(getUserResponseObserver).onCompleted();

        GetUserResponse response = responseCaptor.getValue();
        assertThat(response.hasError()).isTrue();
        assertThat(response.getError().getMessage()).contains("User not found");
    }

    @Test
    void shouldValidateEmailFormat() {
        ValidateEmailRequest validRequest = ValidateEmailRequest.newBuilder()
                .setEmail("valid@example.com")
                .build();

        userService.validateEmail(validRequest, validateEmailResponseObserver);

        ArgumentCaptor<ValidateEmailResponse> responseCaptor = ArgumentCaptor.forClass(ValidateEmailResponse.class);
        verify(validateEmailResponseObserver).onNext(responseCaptor.capture());
        verify(validateEmailResponseObserver).onCompleted();

        ValidateEmailResponse response = responseCaptor.getValue();
        assertThat(response.getIsValid()).isTrue();
        assertThat(response.getIsAvailable()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Email is valid and available");
    }

    @Test
    void shouldRejectInvalidEmailFormat() {
        ValidateEmailRequest invalidRequest = ValidateEmailRequest.newBuilder()
                .setEmail("invalid-email")
                .build();

        userService.validateEmail(invalidRequest, validateEmailResponseObserver);

        ArgumentCaptor<ValidateEmailResponse> responseCaptor = ArgumentCaptor.forClass(ValidateEmailResponse.class);
        verify(validateEmailResponseObserver).onNext(responseCaptor.capture());
        verify(validateEmailResponseObserver).onCompleted();

        ValidateEmailResponse response = responseCaptor.getValue();
        assertThat(response.getIsValid()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Invalid email format");
    }

    @Test
    void shouldCheckUsernameAvailability() {
        CheckUsernameRequest request = CheckUsernameRequest.newBuilder()
                .setUsername("availableuser")
                .build();

        userService.checkUsername(request, checkUsernameResponseObserver);

        ArgumentCaptor<CheckUsernameResponse> responseCaptor = ArgumentCaptor.forClass(CheckUsernameResponse.class);
        verify(checkUsernameResponseObserver).onNext(responseCaptor.capture());
        verify(checkUsernameResponseObserver).onCompleted();

        CheckUsernameResponse response = responseCaptor.getValue();
        assertThat(response.getIsAvailable()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Username is available");
    }

    @Test
    void shouldAuthenticateValidUser() {
        String email = "auth@example.com";
        String password = "password123";

        CreateUserRequest createRequest = CreateUserRequest.newBuilder()
                .setUsername("authuser")
                .setEmail(email)
                .setPassword(password)
                .setFirstName("Auth")
                .setLastName("User")
                .build();

        userService.createUser(createRequest, createUserResponseObserver);

        AuthenticateUserRequest authRequest = AuthenticateUserRequest.newBuilder()
                .setEmail(email)
                .setPassword(password)
                .build();

        userService.authenticateUser(authRequest, authenticateUserResponseObserver);

        ArgumentCaptor<AuthenticateUserResponse> responseCaptor = ArgumentCaptor.forClass(AuthenticateUserResponse.class);
        verify(authenticateUserResponseObserver).onNext(responseCaptor.capture());
        verify(authenticateUserResponseObserver).onCompleted();

        AuthenticateUserResponse response = responseCaptor.getValue();
        assertThat(response.hasUser()).isTrue();
        assertThat(response.getUser().getEmail()).isEqualTo(email);
        assertThat(response.getUser().getUsername()).isEqualTo("authuser");
    }

    @Test
    void shouldRejectInvalidCredentials() {
        AuthenticateUserRequest request = AuthenticateUserRequest.newBuilder()
                .setEmail("nonexistent@example.com")
                .setPassword("wrongpassword")
                .build();

        userService.authenticateUser(request, authenticateUserResponseObserver);

        ArgumentCaptor<AuthenticateUserResponse> responseCaptor = ArgumentCaptor.forClass(AuthenticateUserResponse.class);
        verify(authenticateUserResponseObserver).onNext(responseCaptor.capture());
        verify(authenticateUserResponseObserver).onCompleted();

        AuthenticateUserResponse response = responseCaptor.getValue();
        assertThat(response.hasError()).isTrue();
        assertThat(response.getError().getCode()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(response.getError().getMessage()).isEqualTo("Invalid email or password");
    }

    @Test
    void shouldUpdateUserFields() {
        CreateUserRequest createRequest = CreateUserRequest.newBuilder()
                .setUsername("updateuser")
                .setEmail("update@example.com")
                .setPassword("password123")
                .setFirstName("Update")
                .setLastName("User")
                .build();

        userService.createUser(createRequest, createUserResponseObserver);

        ArgumentCaptor<CreateUserResponse> createResponseCaptor = ArgumentCaptor.forClass(CreateUserResponse.class);
        verify(createUserResponseObserver).onNext(createResponseCaptor.capture());
        String userId = createResponseCaptor.getValue().getUser().getId();

        UpdateUserRequest updateRequest = UpdateUserRequest.newBuilder()
                .setId(userId)
                .setFirstName("Updated")
                .setLastName("Name")
                .build();

        userService.updateUser(updateRequest, updateUserResponseObserver);

        ArgumentCaptor<UpdateUserResponse> updateResponseCaptor = ArgumentCaptor.forClass(UpdateUserResponse.class);
        verify(updateUserResponseObserver).onNext(updateResponseCaptor.capture());
        verify(updateUserResponseObserver).onCompleted();

        UpdateUserResponse response = updateResponseCaptor.getValue();
        assertThat(response.hasUser()).isTrue();
        assertThat(response.getUser().getFirstName()).isEqualTo("Updated");
        assertThat(response.getUser().getLastName()).isEqualTo("Name");
        assertThat(response.getUser().getUsername()).isEqualTo("updateuser");
        assertThat(response.getUser().getEmail()).isEqualTo("update@example.com");
    }
}
