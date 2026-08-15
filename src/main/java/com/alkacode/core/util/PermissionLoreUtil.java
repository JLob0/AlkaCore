package com.alkacode.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Expande uma lista de permissoes de um grupo do LuckPerms (ver LuckPermsHook#getGroupPermissionKeys)
 * numa lista de linhas de lore prontas, uma por permissao, usando nomes amigaveis fornecidos
 * pelo chamador (cada plugin guarda os proprios nomes editaveis, ver PermissionNamesStore).
 * Nao mexe em arquivo nenhum - e uma funcao pura, o I/O fica com quem chama.
 */
public final class PermissionLoreUtil {
    private PermissionLoreUtil() {}

    private static final Pattern TRAILING_NUMBER = Pattern.compile("(\\d+)(?!.*\\d)");

    /**
     * @param permissionKeys chaves cruas (ex: "enderchest.vip.4")
     * @param lineTemplate texto da linha com "%permission%" onde o nome amigavel entra
     *                     (ex: " <gray>- <white>%permission%")
     * @param friendlyNameLookup dado o texto de busca (numero final trocado por "#", ex:
     *                           "enderchest.vip.#"), devolve o nome amigavel ja cadastrado,
     *                           ou null se ainda nao existir
     * @param onUnknownPermission chamado uma vez por chave de busca desconhecida, pra quem
     *                            chamou persistir um valor padrao editavel depois
     */
    public static List<String> expand(List<String> permissionKeys, String lineTemplate,
                                        Function<String, String> friendlyNameLookup,
                                        Consumer<String> onUnknownPermission) {
        List<String> lines = new ArrayList<>();
        for (String permission : permissionKeys) {
            String lookupKey = permission;
            String number = null;
            Matcher matcher = TRAILING_NUMBER.matcher(permission);
            if (matcher.find()) {
                number = matcher.group(1);
                lookupKey = permission.substring(0, matcher.start()) + "#" + permission.substring(matcher.end());
            }
            String friendly = friendlyNameLookup.apply(lookupKey);
            if (friendly == null) {
                onUnknownPermission.accept(lookupKey);
                continue; // sem nome cadastrado ainda - nao mostra a permissao crua pro jogador
            }
            if (friendly.isBlank()) {
                continue; // admin cadastrou vazio de proposito = "nao mostrar essa"
            }
            if (number != null) {
                friendly = friendly.replace("#", number);
            }
            lines.add(lineTemplate.replace("%permission%", friendly));
        }
        return lines;
    }
}
