package services;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthService {
    private Map<String, String> users = new HashMap<>();
    private Map<String, String> activeTokens = new HashMap<>();

    public AuthService() {
        // Simulando um banco de dados de usuários
        users.put("alexa", "senha123"); 
        users.put("cliente1", "1234");
    }

    public String authenticate(String username, String password) {
        if (users.containsKey(username) && users.get(username).equals(password)) {
            String token = UUID.randomUUID().toString(); // Gera um token simples
            activeTokens.put(token, username);
            return token;
        }
        return null;
    }

    public boolean validateToken(String token) {
        return token != null && activeTokens.containsKey(token);
    }
}