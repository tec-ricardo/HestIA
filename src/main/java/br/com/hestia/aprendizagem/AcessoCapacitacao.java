package br.com.hestia.aprendizagem;

import br.com.hestia.reutilizavel.service.AcessoRespostaService;
import br.com.hestia.usuario.model.*;
import br.com.hestia.usuario.repository.UsuarioRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class AcessoCapacitacao {
    private final AcessoRespostaService acesso;
    private final UsuarioRepository usuarios;
    private final JdbcTemplate jdbc;
    public AcessoCapacitacao(AcessoRespostaService acesso,UsuarioRepository usuarios,JdbcTemplate jdbc){this.acesso=acesso;this.usuarios=usuarios;this.jdbc=jdbc;}
    public Long empresa(Usuario autor){return acesso.validarEmpresa(autor,null);}
    public void administrador(Usuario autor){empresa(autor);exigir(autor.getPerfil()==PerfilUsuario.ADMIN,HttpStatus.FORBIDDEN,"Somente ADMIN publica requisitos e concede permissões");}
    public Usuario usuario(Usuario autor,Long id){
        Long empresa=empresa(autor);
        var alvo=usuarios.findById(id).filter(u->Boolean.TRUE.equals(u.getAtivo())&&u.getEmpresa()!=null&&empresa.equals(u.getEmpresa().getId()))
            .orElseThrow(()->erro(HttpStatus.NOT_FOUND,"Usuário indisponível"));
        if(autor.getPerfil()==PerfilUsuario.FUNCIONARIO)exigir(autor.getId().equals(id),HttpStatus.FORBIDDEN,"Acesso restrito ao próprio histórico");
        if(autor.getPerfil()==PerfilUsuario.GESTOR)exigir(autor.getDepartamento()!=null&&alvo.getDepartamento()!=null&&autor.getDepartamento().getId().equals(alvo.getDepartamento().getId()),HttpStatus.FORBIDDEN,"Departamento não autorizado");
        return alvo;
    }
    public void serializar(Long id){jdbc.queryForObject("SELECT id FROM usuarios WHERE id=? FOR UPDATE",Long.class,id);}
    public static void exigir(boolean ok,HttpStatus status,String mensagem){if(!ok)throw erro(status,mensagem);}
    public static ResponseStatusException erro(HttpStatus status,String mensagem){return new ResponseStatusException(status,mensagem);}
    public static String hash(String texto){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
