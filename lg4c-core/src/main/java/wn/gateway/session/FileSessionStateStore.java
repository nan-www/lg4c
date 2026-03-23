package wn.gateway.session;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class FileSessionStateStore {
    private static final TypeReference<List<SessionSnapshot>> LIST_TYPE = new TypeReference<>() {
    };

    private final Path stateFile;
    private final ObjectMapper mapper;

    public FileSessionStateStore(Path stateFile, ObjectMapper mapper) {
        this.stateFile = stateFile;
        this.mapper = mapper.copy().registerModule(new JavaTimeModule());
    }

    public synchronized void save(SessionSnapshot snapshot) throws IOException {
        List<SessionSnapshot> sessions = new ArrayList<>(loadAll());
        sessions.removeIf(existing -> existing.key().equals(snapshot.key()));
        sessions.add(snapshot);
        sessions.sort(Comparator.comparing(entry -> entry.key().value()));
        Files.createDirectories(stateFile.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(stateFile.toFile(), sessions);
    }

    public synchronized List<SessionSnapshot> loadAll() throws IOException {
        if (Files.notExists(stateFile)) {
            return List.of();
        }
        return mapper.readValue(stateFile.toFile(), LIST_TYPE);
    }
}
