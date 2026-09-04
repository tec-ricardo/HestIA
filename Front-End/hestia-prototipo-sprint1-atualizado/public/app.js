const sidebar = document.getElementById("sidebar");

const menuButton = document.getElementById("menuButton");

const toast = document.getElementById("toast");


/* MENU */

if (menuButton) {

    menuButton.addEventListener("click", function() {

        sidebar.classList.toggle("open");

    });

}


/* FECHAR MENU */

const links = document.querySelectorAll(".nav-link");

links.forEach(function(link) {

    link.addEventListener("click", function() {

        sidebar.classList.remove("open");

    });

});


/* SENHA */

const senha = document.getElementById("senha");

const toggleSenha = document.getElementById("toggleSenha");

if (senha && toggleSenha) {

    toggleSenha.addEventListener("click", function() {

        if (senha.type == "password") {

            senha.type = "text";

        }

        else {

            senha.type = "password";

        }

    });

}


/* BOTOES DO PROTOTIPO */

const botoes = document.querySelectorAll(".fake-save");

botoes.forEach(function(botao) {

    botao.addEventListener("click", function() {

        const formulario = botao.closest("form");

        if (formulario && !formulario.checkValidity()) {

            formulario.reportValidity();

            return;

        }


        if (toast) {

            toast.innerHTML =
                "Validação concluída. Protótipo visual: nenhum dado foi gravado.";

            toast.classList.add("show");


            setTimeout(function() {

                toast.classList.remove("show");

            }, 1800);

        }

    });

});

