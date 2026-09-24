package br.com.hestia.usuario.service;

import br.com.hestia.auditoria.model.ResultadoAuditoria;
import br.com.hestia.auditoria.model.TipoAcaoAuditoria;
import br.com.hestia.auditoria.service.LogAuditoriaService;
import br.com.hestia.departamento.model.Departamento;
import br.com.hestia.departamento.repository.DepartamentoRepository;
import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.empresa.repository.EmpresaRepository;
import br.com.hestia.usuario.dto.UsuarioDTO;
import br.com.hestia.usuario.model.Usuario;
import br.com.hestia.usuario.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final DepartamentoRepository departamentoRepository;
    private final PasswordEncoder passwordEncoder;
    private final LogAuditoriaService logAuditoriaService;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            EmpresaRepository empresaRepository,
            DepartamentoRepository departamentoRepository,
            PasswordEncoder passwordEncoder,
            LogAuditoriaService logAuditoriaService
    ) {
        this.usuarioRepository =
                usuarioRepository;

        this.empresaRepository =
                empresaRepository;

        this.departamentoRepository =
                departamentoRepository;

        this.passwordEncoder =
                passwordEncoder;

        this.logAuditoriaService =
                logAuditoriaService;
    }

    public Usuario criar(UsuarioDTO dto) {

        if (usuarioRepository
                .existsByEmail(dto.getEmail())) {

            throw new RuntimeException(
                    "Já existe um usuário cadastrado com esse e-mail."
            );
        }

        Empresa empresa =
                empresaRepository
                        .findById(dto.getEmpresaId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Empresa não encontrada."
                                )
                        );

        Departamento departamento =
                departamentoRepository
                        .findById(
                                dto.getDepartamentoId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Departamento não encontrado."
                                )
                        );

        if (!departamento
                .getEmpresa()
                .getId()
                .equals(empresa.getId())) {

            throw new RuntimeException(
                    "O departamento informado não pertence à empresa selecionada."
            );
        }

        Usuario usuario =
                new Usuario();

        usuario.setNome(dto.getNome());
        usuario.setEmail(dto.getEmail());

        usuario.setSenha(
                passwordEncoder.encode(
                        dto.getSenha()
                )
        );

        usuario.setCargo(dto.getCargo());
        usuario.setPerfil(dto.getPerfil());
        usuario.setEmpresa(empresa);
        usuario.setDepartamento(departamento);

        if (dto.getAtivo() != null) {
            usuario.setAtivo(
                    dto.getAtivo()
            );
        }

        Usuario usuarioSalvo =
                usuarioRepository.save(usuario);

        logAuditoriaService.registrar(
                null,
                TipoAcaoAuditoria.CRIAR,
                "USUARIO",
                usuarioSalvo.getId(),
                "Usuário cadastrado",
                ResultadoAuditoria.SUCESSO,
                null,
                dadosUsuario(usuarioSalvo)
        );

        return usuarioSalvo;
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public Usuario buscarPorId(Long id) {

        return usuarioRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Usuário não encontrado."
                        )
                );
    }

    public Usuario atualizar(
            Long id,
            UsuarioDTO dto
    ) {

        Usuario usuario =
                buscarPorId(id);

        String dadosAnteriores =
                dadosUsuario(usuario);

        Empresa empresa =
                empresaRepository
                        .findById(dto.getEmpresaId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Empresa não encontrada."
                                )
                        );

        Departamento departamento =
                departamentoRepository
                        .findById(
                                dto.getDepartamentoId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Departamento não encontrado."
                                )
                        );

        if (!departamento
                .getEmpresa()
                .getId()
                .equals(empresa.getId())) {

            throw new RuntimeException(
                    "O departamento informado não pertence à empresa selecionada."
            );
        }

        usuario.setNome(dto.getNome());
        usuario.setCargo(dto.getCargo());
        usuario.setPerfil(dto.getPerfil());
        usuario.setEmpresa(empresa);
        usuario.setDepartamento(departamento);

        if (dto.getAtivo() != null) {
            usuario.setAtivo(
                    dto.getAtivo()
            );
        }

        Usuario usuarioSalvo =
                usuarioRepository.save(usuario);

        logAuditoriaService.registrar(
                null,
                TipoAcaoAuditoria.ALTERAR,
                "USUARIO",
                usuarioSalvo.getId(),
                "Usuário atualizado",
                ResultadoAuditoria.SUCESSO,
                dadosAnteriores,
                dadosUsuario(usuarioSalvo)
        );

        return usuarioSalvo;
    }

    public void excluir(Long id) {

        Usuario usuario =
                buscarPorId(id);

        String dadosAnteriores =
                dadosUsuario(usuario);

        usuarioRepository.delete(usuario);

        logAuditoriaService.registrar(
                null,
                TipoAcaoAuditoria.EXCLUIR,
                "USUARIO",
                id,
                "Usuário excluído",
                ResultadoAuditoria.SUCESSO,
                dadosAnteriores,
                null
        );
    }

    private String dadosUsuario(
            Usuario usuario
    ) {

        Long empresaId = null;
        Long departamentoId = null;

        if (usuario.getEmpresa() != null) {
            empresaId =
                    usuario
                            .getEmpresa()
                            .getId();
        }

        if (usuario.getDepartamento() != null) {
            departamentoId =
                    usuario
                            .getDepartamento()
                            .getId();
        }

        return "nome="
                + usuario.getNome()
                + "; email="
                + usuario.getEmail()
                + "; cargo="
                + usuario.getCargo()
                + "; perfil="
                + usuario.getPerfil()
                + "; ativo="
                + usuario.getAtivo()
                + "; empresaId="
                + empresaId
                + "; departamentoId="
                + departamentoId;
    }
}