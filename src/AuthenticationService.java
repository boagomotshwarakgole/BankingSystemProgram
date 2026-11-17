public interface AuthenticationService
{
    boolean authenticate(String username, String password);

    boolean isAuthenticated();

    String getAuthenticatedCustomerId();

    void register(
            String firstName,
            String lastName,
            String nationalId,
            int age,
            String address,
            String phoneNumber,
            String username,
            String email,
            String password);

    String createPasswordResetToken(String email);

    boolean resetPassword(String email, String token, String newPassword);

    void logout();
}
