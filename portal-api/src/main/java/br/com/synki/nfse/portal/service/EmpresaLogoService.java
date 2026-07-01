package br.com.synki.nfse.portal.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class EmpresaLogoService {

    private static final Set<String> EXTENSOES = Set.of("png", "jpg", "jpeg", "gif");
    private final Path baseDir = Path.of("data", "logos");

    public void salvar(Long empresaId, MultipartFile arquivo) throws IOException {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Arquivo de logo obrigatorio");
        }
        var ext = extensao(arquivo.getOriginalFilename());
        if (!EXTENSOES.contains(ext)) {
            throw new IllegalArgumentException("Logo deve ser PNG, JPG ou GIF");
        }
        Files.createDirectories(baseDir);
        removerArquivos(empresaId);
        Files.write(baseDir.resolve(nomeArquivo(empresaId, ext)), arquivo.getBytes());
    }

    public Optional<InputStream> abrir(Long empresaId) throws IOException {
        var arquivo = localizar(empresaId);
        if (arquivo == null) {
            return Optional.empty();
        }
        return Optional.of(new ByteArrayInputStream(Files.readAllBytes(arquivo)));
    }

    public boolean existe(Long empresaId) {
        return localizar(empresaId) != null;
    }

    public void excluir(Long empresaId) throws IOException {
        removerArquivos(empresaId);
    }

    private Path localizar(Long empresaId) {
        for (var ext : EXTENSOES) {
            var path = baseDir.resolve(nomeArquivo(empresaId, ext));
            if (Files.isRegularFile(path)) {
                return path;
            }
        }
        return null;
    }

    private void removerArquivos(Long empresaId) throws IOException {
        if (!Files.isDirectory(baseDir)) {
            return;
        }
        for (var ext : EXTENSOES) {
            Files.deleteIfExists(baseDir.resolve(nomeArquivo(empresaId, ext)));
        }
    }

    private static String nomeArquivo(Long empresaId, String ext) {
        return empresaId + "." + ext;
    }

    private static String extensao(String nome) {
        if (nome == null || !nome.contains(".")) {
            return "";
        }
        return nome.substring(nome.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
