package br.com.hestia.usuario.service;

import br.com.hestia.auditoria.service.AuditoriaService;
import br.com.hestia.departamento.model.Departamento;
import br.com.hestia.departamento.repository.DepartamentoRepository;
import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.empresa.repository.EmpresaRepository;
import br.com.hestia.usuario.dto.UsuarioDTO;
import br.com.hestia.usuario.model.Usuario;
import br.com.hestia.usuario.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final DepartamentoRepository departamentoRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    private final br.com.hestia.autenticacao.repository.SessaoUsuarioRepository sessoes;
    private final br.com.hestia.autenticacao.contexto.EmpresaAtual empresaAtual;

    @Autowired
    public UsuarioService(UsuarioRepository usuarios, EmpresaRepository empresas, DepartamentoRepository departamentos,
            PasswordEncoder encoder, AuditoriaService auditoria,
            br.com.hestia.autenticacao.repository.SessaoUsuarioRepository sessoes,
            br.com.hestia.autenticacao.contexto.EmpresaAtual empresaAtual) {
        this.usuarioRepository=usuarios; this.empresaRepository=empresas; this.departamentoRepository=departamentos;
        this.passwordEncoder=encoder; this.auditoriaService=auditoria; this.sessoes=sessoes; this.empresaAtual=empresaAtual;
    }

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            EmpresaRepository empresaRepository,
            DepartamentoRepository departamentoRepository,
            PasswordEncoder passwordEncoder,
            AuditoriaService auditoriaService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.departamentoRepository = departamentoRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
        this.sessoes = null; this.empresaAtual = null;
    }

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            EmpresaRepository empresaRepository,
            DepartamentoRepository departamentoRepository,
            PasswordEncoder passwordEncoder
    ) {
        this(usuarioRepository, empresaRepository, departamentoRepository, passwordEncoder, null);
    }

    public Usuario criar(UsuarioDTO dto) {

        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException(
                    "Já existe um usuário cadastrado com esse e-mail."
            );
        }

        Empresa empresa = empresaRepository.findById(dto.getEmpresaId())
                .orElseThrow(() ->
                        new RuntimeException("Empresa não encontrada.")
                );

        Departamento departamento =
                departamentoRepository.findById(dto.getDepartamentoId())
                        .orElseThrow(() ->
                                new RuntimeException("Departamento não encontrado.")
                        );

        if (!departamento.getEmpresa().getId().equals(empresa.getId())) {
            throw new RuntimeException(
                    "O departamento informado não pertence à empresa selecionada."
            );
        }

        Usuario usuario = new Usuario();

        usuario.setNome(dto.getNome());
        usuario.setEmail(dto.getEmail());
        usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        usuario.setCargo(dto.getCargo());
        usuario.setPerfil(dto.getPerfil());
        usuario.setEmpresa(empresa);
        usuario.setDepartamento(departamento);

        if (dto.getAtivo() != null) {
            usuario.setAtivo(dto.getAtivo());
        }

        Usuario salvo = usuarioRepository.save(usuario);
        registrar("POST", salvo.getId());
        return salvo;
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Usuário não encontrado.")
                );
    }

    @org.springframework.transaction.annotation.Transactional
    public Usuario atualizar(Long id, UsuarioDTO dto) {

        Usuario usuario = buscarPorId(id);
        if (empresaAtual != null) { empresaAtual.validar(usuario.getEmpresa().getId()); empresaAtual.validar(dto.getEmpresaId()); }
        boolean acessoAlterado = !java.util.Objects.equals(usuario.getPerfil(), dto.getPerfil())
                || !java.util.Objects.equals(usuario.getEmpresa().getId(), dto.getEmpresaId())
                || !java.util.Objects.equals(usuario.getDepartamento().getId(), dto.getDepartamentoId())
                || (dto.getAtivo() != null && !java.util.Objects.equals(usuario.getAtivo(), dto.getAtivo()));

        Empresa empresa = empresaRepository.findById(dto.getEmpresaId())
                .orElseThrow(() ->
                        new RuntimeException("Empresa não encontrada.")
                );

        Departamento departamento =
                departamentoRepository.findById(dto.getDepartamentoId())
                        .orElseThrow(() ->
                                new RuntimeException("Departamento não encontrado.")
                        );

        if (!departamento.getEmpresa().getId().equals(empresa.getId())) {
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
            usuario.setAtivo(dto.getAtivo());
        }

        Usuario salvo = usuarioRepository.save(usuario);
        if (acessoAlterado && sessoes != null) sessoes.revogarTodas(id, java.time.LocalDateTime.now());
        registrar("PUT", salvo.getId());
        return salvo;
    }

    public void excluir(Long id) {

        Usuario usuario = buscarPorId(id);

        usuarioRepository.delete(usuario);
        registrar("DELETE", id);
    }

    private void registrar(String metodo, Long id) {
        if (auditoriaService != null) {
            auditoriaService.registrarOperacao(metodo, "/usuarios/" + id, 200);
        }
    }
}
