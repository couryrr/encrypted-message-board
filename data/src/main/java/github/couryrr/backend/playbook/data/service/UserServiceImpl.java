package github.couryrr.backend.playbook.data.service;

import github.couryrr.backend.playbook.data.gen.user.v1.*;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import com.google.protobuf.Timestamp;

@Component
public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, String> emailToId = new ConcurrentHashMap<>();
    private final Map<String, String> usernameToId = new ConcurrentHashMap<>();
    private final Map<String, String> passwords = new ConcurrentHashMap<>();
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );

    @Override
    public void createUser(CreateUserRequest request, StreamObserver<CreateUserResponse> responseObserver) {
        try {
            ValidationError validationError = validateCreateUserRequest(request);
            if (validationError != null) {
                CreateUserResponse response = CreateUserResponse.newBuilder()
                    .setError(validationError)
                    .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            String userId = UUID.randomUUID().toString();
            Instant now = Instant.now();
            Timestamp timestamp = Timestamp.newBuilder()
                .setSeconds(now.getEpochSecond())
                .setNanos(now.getNano())
                .build();

            User user = User.newBuilder()
                .setId(userId)
                .setUsername(request.getUsername())
                .setEmail(request.getEmail())
                .setFirstName(request.getFirstName())
                .setLastName(request.getLastName())
                .setCreatedAt(timestamp)
                .setUpdatedAt(timestamp)
                .build();

            users.put(userId, user);
            emailToId.put(request.getEmail().toLowerCase(), userId);
            usernameToId.put(request.getUsername().toLowerCase(), userId);
            passwords.put(userId, request.getPassword());

            CreateUserResponse response = CreateUserResponse.newBuilder()
                .setUser(user)
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
        try {
            User user = users.get(request.getId());
            if (user == null) {
                NotFoundError error = NotFoundError.newBuilder()
                    .setMessage("User not found with id: " + request.getId())
                    .build();
                GetUserResponse response = GetUserResponse.newBuilder()
                    .setError(error)
                    .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            GetUserResponse response = GetUserResponse.newBuilder()
                .setUser(user)
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void updateUser(UpdateUserRequest request, StreamObserver<UpdateUserResponse> responseObserver) {
        try {
            User existingUser = users.get(request.getId());
            if (existingUser == null) {
                NotFoundError error = NotFoundError.newBuilder()
                    .setMessage("User not found with id: " + request.getId())
                    .build();
                UpdateUserResponse response = UpdateUserResponse.newBuilder()
                    .setNotFoundError(error)
                    .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            ValidationError validationError = validateUpdateUserRequest(request, existingUser);
            if (validationError != null) {
                UpdateUserResponse response = UpdateUserResponse.newBuilder()
                    .setValidationError(validationError)
                    .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            User.Builder userBuilder = existingUser.toBuilder();
            Instant now = Instant.now();
            Timestamp timestamp = Timestamp.newBuilder()
                .setSeconds(now.getEpochSecond())
                .setNanos(now.getNano())
                .build();
            userBuilder.setUpdatedAt(timestamp);

            if (request.hasUsername()) {
                usernameToId.remove(existingUser.getUsername().toLowerCase());
                usernameToId.put(request.getUsername().toLowerCase(), request.getId());
                userBuilder.setUsername(request.getUsername());
            }
            if (request.hasEmail()) {
                emailToId.remove(existingUser.getEmail().toLowerCase());
                emailToId.put(request.getEmail().toLowerCase(), request.getId());
                userBuilder.setEmail(request.getEmail());
            }
            if (request.hasFirstName()) {
                userBuilder.setFirstName(request.getFirstName());
            }
            if (request.hasLastName()) {
                userBuilder.setLastName(request.getLastName());
            }

            User updatedUser = userBuilder.build();
            users.put(request.getId(), updatedUser);

            UpdateUserResponse response = UpdateUserResponse.newBuilder()
                .setUser(updatedUser)
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void validateEmail(ValidateEmailRequest request, StreamObserver<ValidateEmailResponse> responseObserver) {
        try {
            String email = request.getEmail();
            boolean isValid = EMAIL_PATTERN.matcher(email).matches();
            boolean isAvailable = !emailToId.containsKey(email.toLowerCase());
            
            String message = "";
            if (!isValid) {
                message = "Invalid email format";
            } else if (!isAvailable) {
                message = "Email is already taken";
            } else {
                message = "Email is valid and available";
            }

            ValidateEmailResponse response = ValidateEmailResponse.newBuilder()
                .setIsValid(isValid)
                .setIsAvailable(isAvailable)
                .setMessage(message)
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void checkUsername(CheckUsernameRequest request, StreamObserver<CheckUsernameResponse> responseObserver) {
        try {
            String username = request.getUsername();
            boolean isAvailable = !usernameToId.containsKey(username.toLowerCase());
            
            String message = isAvailable ? "Username is available" : "Username is already taken";

            CheckUsernameResponse response = CheckUsernameResponse.newBuilder()
                .setIsAvailable(isAvailable)
                .setMessage(message)
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void authenticateUser(AuthenticateUserRequest request, StreamObserver<AuthenticateUserResponse> responseObserver) {
        try {
            String userId = emailToId.get(request.getEmail().toLowerCase());
            if (userId == null) {
                AuthenticationError error = AuthenticationError.newBuilder()
                    .setCode("INVALID_CREDENTIALS")
                    .setMessage("Invalid email or password")
                    .build();
                AuthenticateUserResponse response = AuthenticateUserResponse.newBuilder()
                    .setError(error)
                    .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            String storedPassword = passwords.get(userId);
            if (!request.getPassword().equals(storedPassword)) {
                AuthenticationError error = AuthenticationError.newBuilder()
                    .setCode("INVALID_CREDENTIALS")
                    .setMessage("Invalid email or password")
                    .build();
                AuthenticateUserResponse response = AuthenticateUserResponse.newBuilder()
                    .setError(error)
                    .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            User user = users.get(userId);
            AuthenticateUserResponse response = AuthenticateUserResponse.newBuilder()
                .setUser(user)
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    private ValidationError validateCreateUserRequest(CreateUserRequest request) {
        ValidationError.Builder errorBuilder = ValidationError.newBuilder();
        boolean hasErrors = false;

        if (request.getUsername().trim().isEmpty()) {
            errorBuilder.addFieldErrors(FieldError.newBuilder()
                .setField("username")
                .setMessage("Username is required")
                .build());
            hasErrors = true;
        } else if (usernameToId.containsKey(request.getUsername().toLowerCase())) {
            errorBuilder.addFieldErrors(FieldError.newBuilder()
                .setField("username")
                .setMessage("Username is already taken")
                .build());
            hasErrors = true;
        }

        if (request.getEmail().trim().isEmpty()) {
            errorBuilder.addFieldErrors(FieldError.newBuilder()
                .setField("email")
                .setMessage("Email is required")
                .build());
            hasErrors = true;
        } else if (!EMAIL_PATTERN.matcher(request.getEmail()).matches()) {
            errorBuilder.addFieldErrors(FieldError.newBuilder()
                .setField("email")
                .setMessage("Invalid email format")
                .build());
            hasErrors = true;
        } else if (emailToId.containsKey(request.getEmail().toLowerCase())) {
            errorBuilder.addFieldErrors(FieldError.newBuilder()
                .setField("email")
                .setMessage("Email is already taken")
                .build());
            hasErrors = true;
        }

        if (request.getPassword().trim().isEmpty()) {
            errorBuilder.addFieldErrors(FieldError.newBuilder()
                .setField("password")
                .setMessage("Password is required")
                .build());
            hasErrors = true;
        } else if (request.getPassword().length() < 6) {
            errorBuilder.addFieldErrors(FieldError.newBuilder()
                .setField("password")
                .setMessage("Password must be at least 6 characters")
                .build());
            hasErrors = true;
        }

        if (request.getFirstName().trim().isEmpty()) {
            errorBuilder.addFieldErrors(FieldError.newBuilder()
                .setField("first_name")
                .setMessage("First name is required")
                .build());
            hasErrors = true;
        }

        if (request.getLastName().trim().isEmpty()) {
            errorBuilder.addFieldErrors(FieldError.newBuilder()
                .setField("last_name")
                .setMessage("Last name is required")
                .build());
            hasErrors = true;
        }

        if (hasErrors) {
            return errorBuilder
                .setCode("VALIDATION_ERROR")
                .setMessage("Validation failed")
                .build();
        }

        return null;
    }

    private ValidationError validateUpdateUserRequest(UpdateUserRequest request, User existingUser) {
        ValidationError.Builder errorBuilder = ValidationError.newBuilder();
        boolean hasErrors = false;

        if (request.hasUsername() && request.getUsername().trim().isEmpty()) {
            errorBuilder.addFieldErrors(FieldError.newBuilder()
                .setField("username")
                .setMessage("Username cannot be empty")
                .build());
            hasErrors = true;
        } else if (request.hasUsername() && 
                   !request.getUsername().equals(existingUser.getUsername()) &&
                   usernameToId.containsKey(request.getUsername().toLowerCase())) {
            errorBuilder.addFieldErrors(FieldError.newBuilder()
                .setField("username")
                .setMessage("Username is already taken")
                .build());
            hasErrors = true;
        }

        if (request.hasEmail() && request.getEmail().trim().isEmpty()) {
            errorBuilder.addFieldErrors(FieldError.newBuilder()
                .setField("email")
                .setMessage("Email cannot be empty")
                .build());
            hasErrors = true;
        } else if (request.hasEmail() && !EMAIL_PATTERN.matcher(request.getEmail()).matches()) {
            errorBuilder.addFieldErrors(FieldError.newBuilder()
                .setField("email")
                .setMessage("Invalid email format")
                .build());
            hasErrors = true;
        } else if (request.hasEmail() && 
                   !request.getEmail().equals(existingUser.getEmail()) &&
                   emailToId.containsKey(request.getEmail().toLowerCase())) {
            errorBuilder.addFieldErrors(FieldError.newBuilder()
                .setField("email")
                .setMessage("Email is already taken")
                .build());
            hasErrors = true;
        }

        if (hasErrors) {
            return errorBuilder
                .setCode("VALIDATION_ERROR")
                .setMessage("Validation failed")
                .build();
        }

        return null;
    }
}