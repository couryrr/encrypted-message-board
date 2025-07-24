package github.couryrr.backend.playbook.data.service;

import github.couryrr.backend.playbook.data.gen.user.v1.*;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserServiceIntegrationTest {

    @Autowired
    private GrpcChannelFactory channelFactory;

    @Autowired
    private UserServiceImpl userService;

    private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

    @Mock
    private StreamObserver<GetUserResponse> getUserResponseObserver;

    @Mock
    private StreamObserver<ValidateEmailResponse> validateEmailResponseObserver;

    @Mock
    private StreamObserver<CheckUsernameResponse> checkUsernameResponseObserver;

    @Mock
    private StreamObserver<CreateUserResponse> createUserResponseObserver;

    @Mock
    private StreamObserver<AuthenticateUserResponse> authenticateUserResponseObserver;

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

        // Test through network (integration)
        CreateUserResponse networkResponse = userServiceStub.createUser(request);

        assertThat(networkResponse.hasUser()).isTrue();
        assertThat(networkResponse.getUser().getUsername()).isEqualTo("testuser");
        assertThat(networkResponse.getUser().getEmail()).isEqualTo("test@example.com");
        assertThat(networkResponse.getUser().getFirstName()).isEqualTo("Test");
        assertThat(networkResponse.getUser().getLastName()).isEqualTo("User");
        assertThat(networkResponse.getUser().getId()).isNotEmpty();
        assertThat(networkResponse.getUser().hasCreatedAt()).isTrue();
        assertThat(networkResponse.getUser().hasUpdatedAt()).isTrue();

        // Verify using existing service methods (business logic verification)
        String userId = networkResponse.getUser().getId();
        GetUserRequest getUserRequest = GetUserRequest.newBuilder()
                .setId(userId)
                .build();

        userService.getUser(getUserRequest, getUserResponseObserver);

        ArgumentCaptor<GetUserResponse> captor = ArgumentCaptor.forClass(GetUserResponse.class);
        verify(getUserResponseObserver).onNext(captor.capture());
        verify(getUserResponseObserver).onCompleted();

        GetUserResponse getUserResponse = captor.getValue();
        assertThat(getUserResponse.hasUser()).isTrue();
        assertThat(getUserResponse.getUser().getId()).isEqualTo(userId);
        assertThat(getUserResponse.getUser().getUsername()).isEqualTo("testuser");
        assertThat(getUserResponse.getUser().getEmail()).isEqualTo("test@example.com");
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

        // Test through network (integration)
        CreateUserResponse networkResponse = userServiceStub.createUser(request);

        assertThat(networkResponse.hasError()).isTrue();
        assertThat(networkResponse.getError().getCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(networkResponse.getError().getFieldErrorsList()).hasSize(5);

        // Verify business logic directly - same validation should occur
        userService.createUser(request, createUserResponseObserver);

        ArgumentCaptor<CreateUserResponse> captor = ArgumentCaptor.forClass(CreateUserResponse.class);
        verify(createUserResponseObserver).onNext(captor.capture());
        verify(createUserResponseObserver).onCompleted();

        CreateUserResponse directResponse = captor.getValue();
        assertThat(directResponse.hasError()).isTrue();
        assertThat(directResponse.getError().getCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(directResponse.getError().getFieldErrorsList()).hasSize(5);
    }

    @Test
    void shouldValidateEmailCorrectly() {
        ValidateEmailRequest request = ValidateEmailRequest.newBuilder()
                .setEmail("valid@example.com")
                .build();

        // Test through network (integration)
        ValidateEmailResponse networkResponse = userServiceStub.validateEmail(request);

        assertThat(networkResponse.getIsValid()).isTrue();
        assertThat(networkResponse.getIsAvailable()).isTrue();
        assertThat(networkResponse.getMessage()).isEqualTo("Email is valid and available");

        // Verify business logic directly
        userService.validateEmail(request, validateEmailResponseObserver);

        ArgumentCaptor<ValidateEmailResponse> captor = ArgumentCaptor.forClass(ValidateEmailResponse.class);
        verify(validateEmailResponseObserver).onNext(captor.capture());
        verify(validateEmailResponseObserver).onCompleted();

        ValidateEmailResponse directResponse = captor.getValue();
        assertThat(directResponse.getIsValid()).isTrue();
        assertThat(directResponse.getIsAvailable()).isTrue();
        assertThat(directResponse.getMessage()).isEqualTo("Email is valid and available");
    }

    @Test
    void shouldReturnEmailNotAvailableAfterUserCreation() {
        String email = "taken@example.com";
        
        // Create user through network
        CreateUserRequest createRequest = CreateUserRequest.newBuilder()
                .setUsername("testuser")
                .setEmail(email)
                .setPassword("password123")
                .setFirstName("Test")
                .setLastName("User")
                .build();
        userServiceStub.createUser(createRequest);

        // Test email validation through network
        ValidateEmailRequest validateRequest = ValidateEmailRequest.newBuilder()
                .setEmail(email)
                .build();

        ValidateEmailResponse networkResponse = userServiceStub.validateEmail(validateRequest);

        assertThat(networkResponse.getIsValid()).isTrue();
        assertThat(networkResponse.getIsAvailable()).isFalse();
        assertThat(networkResponse.getMessage()).isEqualTo("Email is already taken");

        // Verify business logic directly - should detect the same taken email
        userService.validateEmail(validateRequest, validateEmailResponseObserver);

        ArgumentCaptor<ValidateEmailResponse> captor = ArgumentCaptor.forClass(ValidateEmailResponse.class);
        verify(validateEmailResponseObserver).onNext(captor.capture());
        verify(validateEmailResponseObserver).onCompleted();

        ValidateEmailResponse directResponse = captor.getValue();
        assertThat(directResponse.getIsValid()).isTrue();
        assertThat(directResponse.getIsAvailable()).isFalse();
        assertThat(directResponse.getMessage()).isEqualTo("Email is already taken");
    }

    @Test
    void shouldCheckUsernameAvailability() {
        CheckUsernameRequest request = CheckUsernameRequest.newBuilder()
                .setUsername("availableuser")
                .build();

        // Test through network (integration)
        CheckUsernameResponse networkResponse = userServiceStub.checkUsername(request);

        assertThat(networkResponse.getIsAvailable()).isTrue();
        assertThat(networkResponse.getMessage()).isEqualTo("Username is available");

        // Verify business logic directly
        userService.checkUsername(request, checkUsernameResponseObserver);

        ArgumentCaptor<CheckUsernameResponse> captor = ArgumentCaptor.forClass(CheckUsernameResponse.class);
        verify(checkUsernameResponseObserver).onNext(captor.capture());
        verify(checkUsernameResponseObserver).onCompleted();

        CheckUsernameResponse directResponse = captor.getValue();
        assertThat(directResponse.getIsAvailable()).isTrue();
        assertThat(directResponse.getMessage()).isEqualTo("Username is available");
    }

    @Test
    void shouldReturnUsernameNotAvailableAfterUserCreation() {
        String username = "takenuser";
        
        // Create user through network
        CreateUserRequest createRequest = CreateUserRequest.newBuilder()
                .setUsername(username)
                .setEmail("test@example.com")
                .setPassword("password123")
                .setFirstName("Test")
                .setLastName("User")
                .build();
        userServiceStub.createUser(createRequest);

        // Test username check through network
        CheckUsernameRequest checkRequest = CheckUsernameRequest.newBuilder()
                .setUsername(username)
                .build();

        CheckUsernameResponse networkResponse = userServiceStub.checkUsername(checkRequest);

        assertThat(networkResponse.getIsAvailable()).isFalse();
        assertThat(networkResponse.getMessage()).isEqualTo("Username is already taken");

        // Verify business logic directly - should detect the same taken username
        userService.checkUsername(checkRequest, checkUsernameResponseObserver);

        ArgumentCaptor<CheckUsernameResponse> captor = ArgumentCaptor.forClass(CheckUsernameResponse.class);
        verify(checkUsernameResponseObserver).onNext(captor.capture());
        verify(checkUsernameResponseObserver).onCompleted();

        CheckUsernameResponse directResponse = captor.getValue();
        assertThat(directResponse.getIsAvailable()).isFalse();
        assertThat(directResponse.getMessage()).isEqualTo("Username is already taken");
    }

    @Test
    void shouldGetUserById() {
        // Create user through network
        CreateUserRequest createRequest = CreateUserRequest.newBuilder()
                .setUsername("getuser")
                .setEmail("getuser@example.com")
                .setPassword("password123")
                .setFirstName("Get")
                .setLastName("User")
                .build();
        CreateUserResponse createResponse = userServiceStub.createUser(createRequest);
        String userId = createResponse.getUser().getId();

        // Test get user through network
        GetUserRequest getRequest = GetUserRequest.newBuilder()
                .setId(userId)
                .build();

        GetUserResponse networkResponse = userServiceStub.getUser(getRequest);

        assertThat(networkResponse.hasUser()).isTrue();
        assertThat(networkResponse.getUser().getId()).isEqualTo(userId);
        assertThat(networkResponse.getUser().getUsername()).isEqualTo("getuser");
        assertThat(networkResponse.getUser().getEmail()).isEqualTo("getuser@example.com");

        // Verify business logic directly - should find the same user
        userService.getUser(getRequest, getUserResponseObserver);

        ArgumentCaptor<GetUserResponse> captor = ArgumentCaptor.forClass(GetUserResponse.class);
        verify(getUserResponseObserver).onNext(captor.capture());
        verify(getUserResponseObserver).onCompleted();

        GetUserResponse directResponse = captor.getValue();
        assertThat(directResponse.hasUser()).isTrue();
        assertThat(directResponse.getUser().getId()).isEqualTo(userId);
        assertThat(directResponse.getUser().getUsername()).isEqualTo("getuser");
        assertThat(directResponse.getUser().getEmail()).isEqualTo("getuser@example.com");
    }

    @Test
    void shouldReturnNotFoundForNonExistentUser() {
        GetUserRequest request = GetUserRequest.newBuilder()
                .setId("non-existent-id")
                .build();

        // Test through network (integration)
        GetUserResponse networkResponse = userServiceStub.getUser(request);

        assertThat(networkResponse.hasError()).isTrue();
        assertThat(networkResponse.getError().getMessage()).contains("User not found");

        // Verify business logic directly - should return same error
        userService.getUser(request, getUserResponseObserver);

        ArgumentCaptor<GetUserResponse> captor = ArgumentCaptor.forClass(GetUserResponse.class);
        verify(getUserResponseObserver).onNext(captor.capture());
        verify(getUserResponseObserver).onCompleted();

        GetUserResponse directResponse = captor.getValue();
        assertThat(directResponse.hasError()).isTrue();
        assertThat(directResponse.getError().getMessage()).contains("User not found");
    }

    @Test
    void shouldUpdateUserSuccessfully() {
        // Create user through network
        CreateUserRequest createRequest = CreateUserRequest.newBuilder()
                .setUsername("updateuser")
                .setEmail("update@example.com")
                .setPassword("password123")
                .setFirstName("Update")
                .setLastName("User")
                .build();
        CreateUserResponse createResponse = userServiceStub.createUser(createRequest);
        String userId = createResponse.getUser().getId();

        // Test update through network
        UpdateUserRequest updateRequest = UpdateUserRequest.newBuilder()
                .setId(userId)
                .setFirstName("Updated")
                .setLastName("Name")
                .build();

        UpdateUserResponse networkResponse = userServiceStub.updateUser(updateRequest);

        assertThat(networkResponse.hasUser()).isTrue();
        assertThat(networkResponse.getUser().getFirstName()).isEqualTo("Updated");
        assertThat(networkResponse.getUser().getLastName()).isEqualTo("Name");
        assertThat(networkResponse.getUser().getUsername()).isEqualTo("updateuser");
        assertThat(networkResponse.getUser().getEmail()).isEqualTo("update@example.com");

        // Verify the update persisted by getting user directly
        GetUserRequest getRequest = GetUserRequest.newBuilder()
                .setId(userId)
                .build();

        userService.getUser(getRequest, getUserResponseObserver);

        ArgumentCaptor<GetUserResponse> captor = ArgumentCaptor.forClass(GetUserResponse.class);
        verify(getUserResponseObserver).onNext(captor.capture());
        verify(getUserResponseObserver).onCompleted();

        GetUserResponse getUserResponse = captor.getValue();
        assertThat(getUserResponse.hasUser()).isTrue();
        assertThat(getUserResponse.getUser().getFirstName()).isEqualTo("Updated");
        assertThat(getUserResponse.getUser().getLastName()).isEqualTo("Name");
        assertThat(getUserResponse.getUser().getUsername()).isEqualTo("updateuser");
        assertThat(getUserResponse.getUser().getEmail()).isEqualTo("update@example.com");
    }

    @Test
    void shouldAuthenticateUserSuccessfully() {
        String email = "auth@example.com";
        String password = "password123";
        
        // Create user through network
        CreateUserRequest createRequest = CreateUserRequest.newBuilder()
                .setUsername("authuser")
                .setEmail(email)
                .setPassword(password)
                .setFirstName("Auth")
                .setLastName("User")
                .build();
        userServiceStub.createUser(createRequest);

        // Test authentication through network
        AuthenticateUserRequest authRequest = AuthenticateUserRequest.newBuilder()
                .setEmail(email)
                .setPassword(password)
                .build();

        AuthenticateUserResponse networkResponse = userServiceStub.authenticateUser(authRequest);

        assertThat(networkResponse.hasUser()).isTrue();
        assertThat(networkResponse.getUser().getEmail()).isEqualTo(email);
        assertThat(networkResponse.getUser().getUsername()).isEqualTo("authuser");

        // Verify business logic directly - should authenticate the same user
        userService.authenticateUser(authRequest, authenticateUserResponseObserver);

        ArgumentCaptor<AuthenticateUserResponse> captor = ArgumentCaptor.forClass(AuthenticateUserResponse.class);
        verify(authenticateUserResponseObserver).onNext(captor.capture());
        verify(authenticateUserResponseObserver).onCompleted();

        AuthenticateUserResponse directResponse = captor.getValue();
        assertThat(directResponse.hasUser()).isTrue();
        assertThat(directResponse.getUser().getEmail()).isEqualTo(email);
        assertThat(directResponse.getUser().getUsername()).isEqualTo("authuser");
    }

    @Test
    void shouldReturnAuthenticationErrorForInvalidCredentials() {
        AuthenticateUserRequest request = AuthenticateUserRequest.newBuilder()
                .setEmail("nonexistent@example.com")
                .setPassword("wrongpassword")
                .build();

        // Test through network (integration)
        AuthenticateUserResponse networkResponse = userServiceStub.authenticateUser(request);

        assertThat(networkResponse.hasError()).isTrue();
        assertThat(networkResponse.getError().getCode()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(networkResponse.getError().getMessage()).isEqualTo("Invalid email or password");

        // Verify business logic directly - should return same authentication error
        userService.authenticateUser(request, authenticateUserResponseObserver);

        ArgumentCaptor<AuthenticateUserResponse> captor = ArgumentCaptor.forClass(AuthenticateUserResponse.class);
        verify(authenticateUserResponseObserver).onNext(captor.capture());
        verify(authenticateUserResponseObserver).onCompleted();

        AuthenticateUserResponse directResponse = captor.getValue();
        assertThat(directResponse.hasError()).isTrue();
        assertThat(directResponse.getError().getCode()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(directResponse.getError().getMessage()).isEqualTo("Invalid email or password");
    }
}
