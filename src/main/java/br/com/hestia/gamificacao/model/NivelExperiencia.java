package br.com.hestia.gamificacao.model;

import java.util.Arrays;

public enum NivelExperiencia {
    APRENDIZ(0), CONSCIENTE(100), GUARDIAO(300), EMBAIXADOR(700);
    private final int xpMinimo;
    NivelExperiencia(int xpMinimo) { this.xpMinimo = xpMinimo; }
    public int getXpMinimo() { return xpMinimo; }
    public static NivelExperiencia paraXp(int xp) {
        return Arrays.stream(values()).filter(n -> xp >= n.xpMinimo)
                .reduce((a, b) -> b).orElse(APRENDIZ);
    }
    public NivelExperiencia proximo() {
        int indice = ordinal() + 1;
        return indice < values().length ? values()[indice] : null;
    }
}
