package tn.softsys.buanderie.Chatbot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tn.softsys.buanderie.Chatbot.dto.*;
import tn.softsys.buanderie.Chatbot.tool.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class AiAssistantService {

    // =============================================================
    // CLIENT GROQ
    // =============================================================

    private final RestClient groqRestClient;

    // =============================================================
    // TOOLS METIER
    // =============================================================

    private final StockAssistantTool stockAssistantTool;

    private final StockCritiqueTool stockCritiqueTool;

    private final StockServiceTool stockServiceTool;

    private final CollecteAssistantTool collecteAssistantTool;

    private final LingeAssistantTool lingeAssistantTool;

    // =============================================================
    // JSON
    // =============================================================

    private final ObjectMapper objectMapper;

    // =============================================================
    // MODELE GROQ
    // =============================================================

    @Value("${groq.model}")
    private String model;


    // =============================================================
    // POINT D'ENTREE PRINCIPAL
    // =============================================================

    public AiChatResponse ask(String question) {

        if (question == null || question.isBlank()) {

            return new AiChatResponse(
                    "Veuillez saisir une question."
            );
        }

        // =========================================================
        // 1. Historique de conversation
        // =========================================================

        List<Map<String, Object>> messages =
                new ArrayList<>();


        // =========================================================
        // 2. MESSAGE SYSTEME
        // =========================================================

        messages.add(
                Map.of(
                        "role",
                        "system",

                        "content",
                        """
                        Tu es l'assistant intelligent de la Buanderie hospitalière.

                        Tu aides le responsable à analyser les données réelles
                        et le fonctionnement de la buanderie.

                        ========================================================
                        RÈGLES GÉNÉRALES
                        ========================================================

                        - Utilise obligatoirement les outils disponibles lorsqu'une
                          question nécessite des données réelles de la buanderie.

                        - Ne jamais inventer une donnée provenant de la base de données.

                        - Les données retournées par les outils sont les données
                          officielles de la buanderie.

                        - Respecte strictement les règles métier retournées par
                          le backend.

                        - Ne modifie jamais une valeur, une quantité, un seuil
                          ou un niveau retourné par le backend.

                        - Ne transforme jamais un niveau CRITIQUE en ATTENTION
                          ou inversement.

                        - Ne crée pas de recommandations opérationnelles qui ne
                          sont pas explicitement définies par les données ou
                          les règles métier.

                        - Tu peux reformuler les résultats pour les rendre
                          compréhensibles, mais les valeurs retournées par
                          le backend doivent rester exactement les mêmes.

                        ========================================================
                        RÈGLES POUR LE STOCK
                        ========================================================

                        - getStockSituation est utilisé pour les questions
                          concernant le stock global de toute la buanderie.

                        Exemple :
                        "Quel est le stock actuel ?"

                        Exemple :
                        "Combien de linge avons-nous en stock ?"

                        - getStockByService est utilisé lorsqu'un ou plusieurs
                          services précis sont mentionnés.

                        Exemple :
                        "Quel est le stock du service 17 ?"

                        → utiliser getStockByService avec :
                        serviceIds = [17]

                        Exemple :
                        "Quel est le stock du service 17 et du service 18 ?"

                        → utiliser getStockByService avec :
                        serviceIds = [17,18]

                        Exemple :
                        "Compare le stock des services 17 et 18."

                        → utiliser getStockByService avec :
                        serviceIds = [17,18]

                        - Ne jamais utiliser getStockSituation lorsqu'un ou
                          plusieurs services précis sont mentionnés.

                        ========================================================
                        RÈGLES POUR LES COLLECTES
                        ========================================================

                        - getCollecteAnalyse est utilisé pour toute question
                          concernant les collectes ou le linge collecté.

                        Cela inclut notamment :

                        - nombre de collectes
                        - linge collecté
                        - collectes aujourd'hui
                        - collectes cette semaine
                        - collectes des 30 derniers jours
                        - collectes d'un service
                        - collectes de plusieurs services
                        - collectes d'un agent
                        - collectes infectées
                        - collectes blanches
                        - collectes mixtes
                        - collectes ouvertes
                        - collectes terminées
                        - quantités attendues
                        - quantités scannées
                        - quantités propres
                        - quantités infectées
                        - quantités manquantes
                        - répartition par catégorie.

                        Si plusieurs services sont mentionnés dans une question
                        sur les collectes, utiliser serviceIds.

                        Exemple :

                        "Combien de linge a été collecté par le service 17 ?"

                        → serviceIds = [17]

                        Exemple :

                        "Compare les collectes des services 17 et 18."

                        → serviceIds = [17,18]

                        Si une période est mentionnée :

                        "aujourd'hui"
                        → periode = AUJOURD_HUI

                        "cette semaine"
                        → periode = 7_DERNIERS_JOURS

                        "les 30 derniers jours"
                        → periode = 30_DERNIERS_JOURS

                        Si aucun filtre n'est précisé, ne crée pas de filtre
                        inutile.

                        Si l'utilisateur demande une comparaison entre plusieurs
                        services, utiliser un seul appel avec tous les serviceIds.

                        Ne réponds jamais avec des quantités de collecte sans
                        avoir consulté getCollecteAnalyse.

                        ========================================================
                        RÈGLES POUR LES LINGES
                        ========================================================

                        getLingeAnalyse est utilisé pour les questions concernant
                        l'état actuel ou le cycle de vie des linges.

                        Correspondances obligatoires :

                        - "pliage"
                        - "en pliage"
                        - "linge au pliage"
                        - "linges au pliage"
                        → PLIE

                        - "lavage"
                        - "en lavage"
                        → EN_LAVAGE

                        - "séchage"
                        - "en séchage"
                        → EN_SECHAGE

                        - "séché"
                        - "sec"
                        → SECHE

                        - "tri"
                        - "en tri"
                        → EN_TRI

                        - "réception"
                        - "en réception"
                        → RECEPTION

                        - "collecte"
                        - "en collecte"
                        → COLLECTE

                        - "stock"
                        - "en stock"
                        - "stocké"
                        → STOCKE

                        - "livré"
                        - "livraison"
                        → LIVRE

                        Exemples :

                        "Combien de linges sont en lavage ?"
                        → etats = ["EN_LAVAGE"]

                        "Combien de draps sont en lavage ?"
                        → etats = ["EN_LAVAGE"]
                        → categories = ["DRAP"]

                        "Quels linges sont bloqués ?"
                        → bloque = true

                        "Quels linges sont bloqués en lavage ?"
                        → etats = ["EN_LAVAGE"]
                        → bloque = true

                        "Quels linges sont bloqués depuis plus de 24 heures ?"
                        → bloque = true
                        → seuilBlocageHeures = 24

                        "Compare les linges en lavage des services 17 et 18."
                        → etats = ["EN_LAVAGE"]
                        → serviceIds = [17,18]

                        "Donne-moi le circuit complet du linge."
                        → utiliser getLingeAnalyse sans filtre.

                        ========================================================
                        RÈGLES IMPORTANTES SUR LES ENUMS
                        ========================================================

                        Ne jamais inventer un état.

                        L'état "PLIAGE" n'existe pas dans le backend.

                        Pour "pliage", utiliser obligatoirement :
                        PLIE

                        ========================================================
                        RÈGLES DE RÉPONSE
                        ========================================================

                        - Réponds toujours en français.

                        - Sois clair, professionnel et concis.

                        - Utilise des listes lorsque cela améliore la lisibilité.

                        - Pour une comparaison entre plusieurs services,
                          présente séparément les résultats de chaque service.

                        - Si les données retournées sont nulles ou à zéro,
                          indique-le clairement.

                        - Ne parle pas de la mécanique interne des tools,
                          sauf si l'utilisateur le demande explicitement.

                        - Ne prétends jamais connaître une information qui
                          n'a pas été retournée par un outil.
                        """
                )
        );


        // =========================================================
        // 3. QUESTION UTILISATEUR
        // =========================================================

        messages.add(
                Map.of(
                        "role",
                        "user",

                        "content",
                        question
                )
        );


        // =========================================================
        // 4. DEFINITION DES TOOLS
        // =========================================================

        List<Map<String, Object>> tools =
                construireTools();


        // =========================================================
        // 5. PREMIER APPEL GROQ
        // =========================================================

        Map<String, Object> response =
                appelerGroq(
                        messages,
                        tools
                );


        // =========================================================
        // 6. RECUPERATION DU MESSAGE
        // =========================================================

        Map<String, Object> assistantMessage =
                recupererAssistantMessage(response);


        // =========================================================
        // 7. RECUPERATION DES TOOL CALLS
        // =========================================================

        List<Map<String, Object>> toolCalls =
                (List<Map<String, Object>>)
                        assistantMessage.get("tool_calls");


        // =========================================================
        // 8. AUCUN TOOL
        // =========================================================

        if (toolCalls == null || toolCalls.isEmpty()) {

            String content =
                    (String)
                            assistantMessage.get("content");

            if (content == null || content.isBlank()) {

                return new AiChatResponse(
                        "Je n'ai pas pu générer une réponse."
                );
            }

            return new AiChatResponse(
                    content
            );
        }


        // =========================================================
        // 9. AJOUT MESSAGE ASSISTANT
        // =========================================================

        messages.add(
                assistantMessage
        );


        // =========================================================
        // 10. EXECUTION DES TOOLS
        // =========================================================

        for (Map<String, Object> toolCall : toolCalls) {

            String toolCallId =
                    (String)
                            toolCall.get("id");

            Map<String, Object> function =
                    (Map<String, Object>)
                            toolCall.get("function");

            if (function == null) {

                throw new RuntimeException(
                        "Fonction absente dans le tool call Groq."
                );
            }

            String functionName =
                    (String)
                            function.get("name");

            String arguments =
                    (String)
                            function.get("arguments");

            if (functionName == null ||
                    functionName.isBlank()) {

                throw new RuntimeException(
                        "Nom du tool absent dans la réponse Groq."
                );
            }

            if (arguments == null ||
                    arguments.isBlank()) {

                arguments = "{}";
            }


            // -----------------------------------------------------
            // EXECUTION BACKEND
            // -----------------------------------------------------

            String toolResult =
                    executeTool(
                            functionName,
                            arguments
                    );


            // -----------------------------------------------------
            // RESULTAT DU TOOL → GROQ
            // -----------------------------------------------------

            messages.add(
                    Map.of(
                            "role",
                            "tool",

                            "tool_call_id",
                            toolCallId,

                            "content",
                            toolResult
                    )
            );
        }


        // =========================================================
        // 11. DEUXIEME APPEL GROQ
        // =========================================================

        Map<String, Object> finalResponse =
                appelerGroq(
                        messages,
                        tools
                );


        // =========================================================
        // 12. MESSAGE FINAL
        // =========================================================

        Map<String, Object> finalMessage =
                recupererAssistantMessage(
                        finalResponse
                );


        String finalContent =
                (String)
                        finalMessage.get("content");


        if (finalContent == null ||
                finalContent.isBlank()) {

            return new AiChatResponse(
                    "Je n'ai pas pu générer une réponse à partir des données disponibles."
            );
        }


        return new AiChatResponse(
                finalContent
        );
    }


    // =============================================================
    // APPEL GROQ
    // =============================================================

    private Map<String, Object> appelerGroq(
            List<Map<String, Object>> messages,
            List<Map<String, Object>> tools
    ) {

        Map<String, Object> body = new HashMap<>();

        body.put("model", model);
        body.put("messages", messages);
        body.put("tools", tools);
        body.put("tool_choice", "auto");

        ResponseEntity<Map> response = groqRestClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(Map.class);

        Map<String, Object> responseBody = response.getBody();

        // ========================================
        // TOKENS UTILISÉS
        // ========================================

        if (responseBody != null) {

            Map<String, Object> usage =
                    (Map<String, Object>) responseBody.get("usage");

            if (usage != null) {

                Object promptTokens =
                        usage.get("prompt_tokens");

                Object completionTokens =
                        usage.get("completion_tokens");

                Object totalTokens =
                        usage.get("total_tokens");

                System.out.println();
                System.out.println("╔════════════════════════════════════════════╗");
                System.out.println("║             GROQ TOKEN USAGE               ║");
                System.out.println("╠════════════════════════════════════════════╣");
                System.out.println(
                        "║ Prompt tokens     : " + promptTokens
                );
                System.out.println(
                        "║ Completion tokens : " + completionTokens
                );
                System.out.println(
                        "║ Total tokens      : " + totalTokens
                );
                System.out.println("╚════════════════════════════════════════════╝");
                System.out.println();
            }
        }

        // ========================================
        // RATE LIMITS
        // ========================================

        HttpHeaders headers = response.getHeaders();

        String remainingRequests =
                headers.getFirst("x-ratelimit-remaining-requests");

        String limitRequests =
                headers.getFirst("x-ratelimit-limit-requests");

        String resetRequests =
                headers.getFirst("x-ratelimit-reset-requests");

        String remainingTokens =
                headers.getFirst("x-ratelimit-remaining-tokens");

        String limitTokens =
                headers.getFirst("x-ratelimit-limit-tokens");

        String resetTokens =
                headers.getFirst("x-ratelimit-reset-tokens");

        System.out.println();
        System.out.println("╔════════════════════════════════════════════╗");
        System.out.println("║             GROQ RATE LIMIT                ║");
        System.out.println("╠════════════════════════════════════════════╣");

        System.out.println(
                "║ Requests restantes : "
                        + remainingRequests
                        + " / "
                        + limitRequests
        );

        System.out.println(
                "║ Tokens restants    : "
                        + remainingTokens
                        + " / "
                        + limitTokens
        );

        System.out.println(
                "║ Reset requests     : "
                        + resetRequests
        );

        System.out.println(
                "║ Reset tokens       : "
                        + resetTokens
        );

        System.out.println("╚════════════════════════════════════════════╝");
        System.out.println();

        return responseBody;
    }

    // =============================================================
    // RECUPERER MESSAGE ASSISTANT
    // =============================================================

    private Map<String, Object> recupererAssistantMessage(
            Map<String, Object> response
    ) {

        List<Map<String, Object>> choices =
                (List<Map<String, Object>>)
                        response.get("choices");


        if (choices == null ||
                choices.isEmpty()) {

            throw new RuntimeException(
                    "Aucun choix reçu depuis Groq."
            );
        }


        Map<String, Object> message =
                (Map<String, Object>)
                        choices
                                .get(0)
                                .get("message");


        if (message == null) {

            throw new RuntimeException(
                    "Message assistant absent dans la réponse Groq."
            );
        }


        return message;
    }


    // =============================================================
    // CONSTRUCTION DES TOOLS
    // =============================================================

    private List<Map<String, Object>> construireTools() {

        return List.of(

                // =====================================================
                // STOCK GLOBAL
                // =====================================================

                Map.of(
                        "type",
                        "function",

                        "function",
                        Map.of(
                                "name",
                                "getStockSituation",

                                "description",
                                """
                                Récupère la situation réelle du stock
                                de linge actuellement stocké dans toute
                                la buanderie.

                                Retourne :
                                - le nombre total de linges en stock
                                - la quantité par catégorie.

                                Utiliser uniquement pour le stock global.
                                """,

                                "parameters",
                                Map.of(
                                        "type",
                                        "object",
                                        "properties",
                                        Map.of(),
                                        "additionalProperties",
                                        false
                                )
                        )
                ),


                // =====================================================
                // STOCK CRITIQUE
                // =====================================================

                Map.of(
                        "type",
                        "function",

                        "function",
                        Map.of(
                                "name",
                                "getStockCritique",

                                "description",
                                """
                                Retourne les catégories de linge dont le
                                stock est CRITIQUE ou ATTENTION selon les
                                seuils métier définis dans le backend.

                                Le backend détermine le niveau.
                                Ne jamais modifier le niveau retourné.
                                """,

                                "parameters",
                                Map.of(
                                        "type",
                                        "object",
                                        "properties",
                                        Map.of(),
                                        "additionalProperties",
                                        false
                                )
                        )
                ),


                // =====================================================
                // STOCK PAR SERVICE
                // =====================================================

                Map.of(
                        "type",
                        "function",

                        "function",
                        Map.of(
                                "name",
                                "getStockByService",

                                "description",
                                """
                                Retourne la situation réelle du stock
                                d'un ou plusieurs services.

                                Utiliser obligatoirement cet outil lorsqu'un
                                service précis est mentionné.

                                Exemple :
                                service 17 → serviceIds = [17]

                                Exemple :
                                services 17 et 18 → serviceIds = [17,18]
                                """,

                                "parameters",
                                Map.of(
                                        "type",
                                        "object",

                                        "properties",
                                        Map.of(
                                                "serviceIds",
                                                Map.of(
                                                        "type",
                                                        "array",
                                                        "description",
                                                        "Identifiants des services.",
                                                        "items",
                                                        Map.of(
                                                                "type",
                                                                "integer"
                                                        )
                                                )
                                        ),

                                        "required",
                                        List.of(
                                                "serviceIds"
                                        ),

                                        "additionalProperties",
                                        false
                                )
                        )
                ),


                // =====================================================
                // COLLECTES
                // =====================================================

                Map.of(
                        "type",
                        "function",

                        "function",
                        Map.of(
                                "name",
                                "getCollecteAnalyse",

                                "description",
                                """
                                Analyse les collectes réelles de la buanderie.

                                Utiliser pour les questions concernant :
                                - collectes
                                - linge collecté
                                - collectes aujourd'hui
                                - collectes cette semaine
                                - collectes des 30 derniers jours
                                - services
                                - agents
                                - collectes infectées
                                - collectes blanches
                                - collectes mixtes
                                - collectes ouvertes
                                - collectes terminées
                                - quantités attendues
                                - quantités scannées
                                - quantités propres
                                - quantités infectées
                                - quantités manquantes
                                """,

                                "parameters",
                                Map.of(
                                        "type",
                                        "object",

                                        "properties",
                                        Map.of(

                                                "periode",
                                                Map.of(
                                                        "type",
                                                        "string",

                                                        "enum",
                                                        List.of(
                                                                "AUJOURD_HUI",
                                                                "7_DERNIERS_JOURS",
                                                                "30_DERNIERS_JOURS",
                                                                "TOUTES"
                                                        )
                                                ),

                                                "serviceIds",
                                                Map.of(
                                                        "type",
                                                        "array",
                                                        "items",
                                                        Map.of(
                                                                "type",
                                                                "integer"
                                                        )
                                                ),

                                                "personnelId",
                                                Map.of(
                                                        "type",
                                                        "integer"
                                                ),

                                                "typeCollecte",
                                                Map.of(
                                                        "type",
                                                        "string"
                                                ),

                                                "etatCollecte",
                                                Map.of(
                                                        "type",
                                                        "string"
                                                )
                                        ),

                                        "additionalProperties",
                                        false
                                )
                        )
                ),


                // =====================================================
                // ANALYSE LINGE
                // =====================================================

                Map.of(
                        "type",
                        "function",

                        "function",
                        Map.of(
                                "name",
                                "getLingeAnalyse",

                                "description",
                                """
                                Analyse le cycle de vie réel des linges.

                                Utiliser pour :
                                - états des linges
                                - collecte
                                - réception
                                - tri
                                - lavage
                                - séchage
                                - séchage terminé
                                - pliage
                                - stock
                                - livraison
                                - linge infecté
                                - linge manquant
                                - linges bloqués.

                                Correspondances :
                                pliage → PLIE
                                lavage → EN_LAVAGE
                                séchage → EN_SECHAGE
                                séché → SECHE
                                tri → EN_TRI
                                réception → RECEPTION
                                collecte → COLLECTE
                                stock → STOCKE
                                livré → LIVRE
                                """,

                                "parameters",
                                Map.of(
                                        "type",
                                        "object",

                                        "properties",
                                        Map.of(

                                                "etats",
                                                Map.of(
                                                        "type",
                                                        "array",
                                                        "items",
                                                        Map.of(
                                                                "type",
                                                                "string"
                                                        )
                                                ),

                                                "categories",
                                                Map.of(
                                                        "type",
                                                        "array",
                                                        "items",
                                                        Map.of(
                                                                "type",
                                                                "string"
                                                        )
                                                ),

                                                "serviceIds",
                                                Map.of(
                                                        "type",
                                                        "array",
                                                        "items",
                                                        Map.of(
                                                                "type",
                                                                "integer"
                                                        )
                                                ),

                                                "bloque",
                                                Map.of(
                                                        "type",
                                                        "boolean"
                                                ),

                                                "seuilBlocageHeures",
                                                Map.of(
                                                        "type",
                                                        "integer"
                                                )
                                        ),

                                        "additionalProperties",
                                        false
                                )
                        )
                )
        );
    }


    // =============================================================
    // EXECUTION DES TOOLS
    // =============================================================

    private String executeTool(
            String functionName,
            String arguments
    ) {

        try {

            switch (functionName) {

                // =================================================
                // STOCK GLOBAL
                // =================================================

                case "getStockSituation":

                    StockSituationResponse stock =
                            stockAssistantTool
                                    .getStockSituation();

                    return objectMapper.writeValueAsString(
                            stock
                    );


                // =================================================
                // STOCK CRITIQUE
                // =================================================

                case "getStockCritique":

                    List<StockCritiqueResponse>
                            stockCritique =
                            stockCritiqueTool
                                    .getStockCritique();

                    return objectMapper.writeValueAsString(
                            stockCritique
                    );


                // =================================================
                // STOCK PAR SERVICES
                // =================================================

                case "getStockByService":

                    Map<String, Object>
                            stockArguments =
                            objectMapper.readValue(
                                    arguments,
                                    Map.class
                            );

                    Object serviceIdsObject =
                            stockArguments.get(
                                    "serviceIds"
                            );

                    if (serviceIdsObject == null) {

                        throw new RuntimeException(
                                "La liste serviceIds est obligatoire."
                        );
                    }

                    List<Integer>
                            serviceIdsInteger =
                            (List<Integer>)
                                    serviceIdsObject;

                    if (serviceIdsInteger.isEmpty()) {

                        throw new RuntimeException(
                                "Au moins un service doit être fourni."
                        );
                    }

                    List<Long> serviceIds =
                            serviceIdsInteger
                                    .stream()
                                    .map(
                                            Integer::longValue
                                    )
                                    .toList();

                    List<StockServiceResponse>
                            stocksServices =
                            stockServiceTool
                                    .getStockByServices(
                                            serviceIds
                                    );

                    return objectMapper.writeValueAsString(
                            stocksServices
                    );


                // =================================================
                // ANALYSE LINGE
                // =================================================

                case "getLingeAnalyse":

                    Map<String, Object>
                            lingeArguments =
                            objectMapper.readValue(
                                    arguments,
                                    Map.class
                            );


                    List<String> etats = null;

                    Object etatsObject =
                            lingeArguments.get("etats");

                    if (etatsObject != null) {

                        etats =
                                (List<String>)
                                        etatsObject;
                    }


                    List<String> categories = null;

                    Object categoriesObject =
                            lingeArguments.get("categories");

                    if (categoriesObject != null) {

                        categories =
                                (List<String>)
                                        categoriesObject;
                    }


                    List<Long> lingeServiceIds = null;

                    Object lingeServiceIdsObject =
                            lingeArguments.get(
                                    "serviceIds"
                            );

                    if (lingeServiceIdsObject != null) {

                        List<Integer> ids =
                                (List<Integer>)
                                        lingeServiceIdsObject;

                        lingeServiceIds =
                                ids.stream()
                                        .map(
                                                Integer::longValue
                                        )
                                        .toList();
                    }


                    Boolean bloque = null;

                    Object bloqueObject =
                            lingeArguments.get(
                                    "bloque"
                            );

                    if (bloqueObject != null) {

                        bloque =
                                (Boolean)
                                        bloqueObject;
                    }


                    Long seuilBlocageHeures = null;

                    Object seuilObject =
                            lingeArguments.get(
                                    "seuilBlocageHeures"
                            );

                    if (seuilObject != null) {

                        seuilBlocageHeures =
                                ((Number)
                                        seuilObject)
                                        .longValue();
                    }


                    LingeAnalyseResponse lingeAnalyse =
                            lingeAssistantTool.analyser(
                                    etats,
                                    categories,
                                    lingeServiceIds,
                                    bloque,
                                    seuilBlocageHeures
                            );


                    return objectMapper.writeValueAsString(
                            lingeAnalyse
                    );


                // =================================================
                // ANALYSE COLLECTES
                // =================================================

                case "getCollecteAnalyse":

                    Map<String, Object>
                            collecteArguments =
                            objectMapper.readValue(
                                    arguments,
                                    Map.class
                            );


                    String periode =
                            (String)
                                    collecteArguments.get(
                                            "periode"
                                    );


                    List<Long> collecteServiceIds =
                            null;

                    Object collecteServiceIdsObject =
                            collecteArguments.get(
                                    "serviceIds"
                            );

                    if (collecteServiceIdsObject != null) {

                        List<Integer> ids =
                                (List<Integer>)
                                        collecteServiceIdsObject;

                        if (!ids.isEmpty()) {

                            collecteServiceIds =
                                    ids.stream()
                                            .map(
                                                    Integer::longValue
                                            )
                                            .toList();
                        }
                    }


                    Long personnelId = null;

                    Object personnelIdObject =
                            collecteArguments.get(
                                    "personnelId"
                            );

                    if (personnelIdObject != null) {

                        personnelId =
                                ((Number)
                                        personnelIdObject)
                                        .longValue();
                    }


                    String typeCollecte =
                            (String)
                                    collecteArguments.get(
                                            "typeCollecte"
                                    );


                    String etatCollecte =
                            (String)
                                    collecteArguments.get(
                                            "etatCollecte"
                                    );


                    CollecteAnalyseResponse analyse =
                            collecteAssistantTool.analyser(
                                    periode,
                                    collecteServiceIds,
                                    personnelId,
                                    typeCollecte,
                                    etatCollecte
                            );


                    return objectMapper.writeValueAsString(
                            analyse
                    );


                // =================================================
                // TOOL INCONNU
                // =================================================

                default:

                    throw new RuntimeException(
                            "Tool inconnu : "
                                    + functionName
                    );
            }


        } catch (JsonProcessingException e) {

            throw new RuntimeException(
                    "Erreur lors du traitement des arguments du tool.",
                    e
            );
        }
    }
}