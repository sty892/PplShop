package styy.pplShop.pplshop.client.model;

import java.util.List;

public record ItemResolutionTrace(
        String selectedCandidate,
        String selectedCandidateSource,
        String selectedResolver,
        String selectedItemId,
        String matchedAlias,
        String fallbackReason,
        List<String> suggestedAliases,
        List<String> consideredCandidates,
        List<String> rejectedCandidates
) {
    public ItemResolutionTrace {
        selectedCandidate = safe(selectedCandidate);
        selectedCandidateSource = safe(selectedCandidateSource);
        selectedResolver = safe(selectedResolver);
        selectedItemId = safe(selectedItemId);
        matchedAlias = safe(matchedAlias);
        fallbackReason = safe(fallbackReason);
        suggestedAliases = suggestedAliases == null ? List.of() : List.copyOf(suggestedAliases);
        consideredCandidates = consideredCandidates == null ? List.of() : List.copyOf(consideredCandidates);
        rejectedCandidates = rejectedCandidates == null ? List.of() : List.copyOf(rejectedCandidates);
    }

    public static ItemResolutionTrace empty() {
        return new ItemResolutionTrace("", "", "", "", "", "", List.of(), List.of(), List.of());
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
