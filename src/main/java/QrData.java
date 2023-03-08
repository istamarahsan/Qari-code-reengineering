import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class QrData {
    private final Map<String, String> savedData = new HashMap<>();
    
    public void store(String name, String content) {
        savedData.put(name, content);
    }
    public Optional<String> retrieve(String name) {
        return Optional.ofNullable(savedData.getOrDefault(name, null));
    }
}
