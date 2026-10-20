package br.com.hestia.rag.model;

import br.com.hestia.reutilizavel.model.RespostaReutilizavel;
import jakarta.persistence.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Entity
@Table(
        name = "resposta_versoes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_resposta_numero_versao",
                columnNames = {"resposta_id", "numero_versao"}
        )
)
public class RespostaVersao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resposta_id", nullable = false, updatable = false)
    private RespostaReutilizavel resposta;

    @Column(name = "numero_versao", nullable = false, updatable = false)
    private Integer numeroVersao;

    @Column(nullable = false, length = 500, updatable = false)
    private String origem;

    @Column(
            name = "tipo_conteudo",
            nullable = false,
            length = 50,
            updatable = false
    )
    private String tipoConteudo;

    @Column(
            name = "conteudo_sha256",
            nullable = false,
            length = 64,
            updatable = false
    )
    private String conteudoSha256;

    @Column(
            name = "prompt_original",
            nullable = false,
            columnDefinition = "TEXT",
            updatable = false
    )
    private String promptOriginal;

    @Column(
            name = "resposta_gerada",
            nullable = false,
            columnDefinition = "TEXT",
            updatable = false
    )
    private String respostaGerada;

    @Column(name = "criada_em", nullable = false, updatable = false)
    private LocalDateTime criadaEm;

    protected RespostaVersao() {
    }

    public RespostaVersao(
            RespostaReutilizavel resposta,
            int numeroVersao,
            String origem,
            String tipoConteudo
    ) {
        if (resposta == null || resposta.getId() == null) {
            throw new IllegalArgumentException(
                    "A resposta deve estar persistida."
            );
        }

        if (numeroVersao < 1) {
            throw new IllegalArgumentException(
                    "A versão deve ser maior que zero."
            );
        }

        this.resposta = resposta;
        this.numeroVersao = numeroVersao;
        this.origem = validarTexto(origem, "Origem", 500);
        this.tipoConteudo = validarTexto(
                tipoConteudo, "Tipo de conteúdo", 50
        );
        this.promptOriginal = validarTexto(
                resposta.getPromptOriginal(), "Prompt", Integer.MAX_VALUE
        );
        this.respostaGerada = validarTexto(
                resposta.getRespostaGerada(), "Resposta", Integer.MAX_VALUE
        );
        this.conteudoSha256 = calcularHash(
                promptOriginal, respostaGerada
        );
        this.criadaEm = LocalDateTime.now();
    }

    private static String validarTexto(
            String valor,
            String campo,
            int tamanhoMaximo
    ) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(
                    campo + " é obrigatório."
            );
        }

        if (valor.length() > tamanhoMaximo) {
            throw new IllegalArgumentException(
                    campo + " excede o tamanho permitido."
            );
        }

        return valor;
    }

    private static String calcularHash(String prompt, String resposta) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] promptBytes = prompt.getBytes(StandardCharsets.UTF_8);
            byte[] respostaBytes = resposta.getBytes(StandardCharsets.UTF_8);

            digest.update(
                    java.nio.ByteBuffer.allocate(Integer.BYTES)
                            .putInt(promptBytes.length)
                            .array()
            );
            digest.update(promptBytes);
            digest.update(respostaBytes);

            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 não disponível.", exception
            );
        }
    }

    public Long getId() {
        return id;
    }

    public RespostaReutilizavel getResposta() {
        return resposta;
    }

    public Integer getNumeroVersao() {
        return numeroVersao;
    }

    public String getOrigem() {
        return origem;
    }

    public String getTipoConteudo() {
        return tipoConteudo;
    }

    public String getConteudoSha256() {
        return conteudoSha256;
    }

    public String getPromptOriginal() {
        return promptOriginal;
    }

    public String getRespostaGerada() {
        return respostaGerada;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }
}