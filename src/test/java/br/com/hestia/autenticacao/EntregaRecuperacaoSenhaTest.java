package br.com.hestia.autenticacao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import br.com.hestia.autenticacao.service.EntregaRecuperacaoSenha;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class EntregaRecuperacaoSenhaTest {
 @Test void enviaLinkSeguroSemPersistirTokenNaMensagemDeErro(){
  ObjectProvider<JavaMailSender> p=mock(ObjectProvider.class);var sender=mock(JavaMailSender.class);when(p.getIfAvailable()).thenReturn(sender);
  var service=new EntregaRecuperacaoSenha(p,"suporte@example.test","https://hestia.example.test");service.enviar("usuario@example.test","segredo");
  var mensagem=org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);verify(sender).send(mensagem.capture());assertTrue(mensagem.getValue().getText().contains("/redefinir-senha?token=segredo"));
  doThrow(new org.springframework.mail.MailSendException("segredo interno")).when(sender).send(any(SimpleMailMessage.class));
  var erro=assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.enviar("usuario@example.test","segredo"));assertFalse(erro.getMessage().contains("segredo"));
 }
 @Test void exigeConfiguracaoReal(){ObjectProvider<JavaMailSender> p=mock(ObjectProvider.class);assertThrows(org.springframework.web.server.ResponseStatusException.class,()->new EntregaRecuperacaoSenha(p,"","").enviar("usuario@example.test","token"));}
}
