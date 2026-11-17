# Team Project

The readme should include information such as:
- a summary of what your application is all about
- a list of the user stories, along with who is responsible for each one
- information about the API(s) that your project uses 
- screenshots or animations demonstrating current functionality

### User stories

| ID | User story | Scope | Lead |
| --- | --- | --- | --- |
| MM-1 | As a user, I want to receive a recommended profile that satisfies both mine and a potential partner's preferences and deal-breakers, so that I can find a potential match without browsing unsuitable profiles. | MVP: deterministic candidate selection and a displayed preview. | Mike Chen |
| MM-2 | As a user, I want future recommendations to reflect the profiles I like, pass or explicitly dislike so that the feed becomes more relevant to me. | Later: feedback-based personalization, including stronger positive feedback from confirmed matches. | Mike Chen |

### Use case: Receive a potential-match recommendation

**Related story:** MM-1

**Primary actor:** Signed-in user

**Lead:** Mike Chen

**Preconditions:** The user has an active, published profile and an authenticated session.

**Trigger:** The user enters their explore page.

**Success outcome:** The user sees one eligible candidate's preview.

**Main success scenario**

1. The user requests a recommendation.
2. The system verifies that the user's session and profile are still available for discovery.
3. The system identifies candidates who satisfy both people's hard preferences, excluding the user, unavailable profiles, blocks in either direction and candidates suppressed by interaction history.
4. The system ranks the eligible candidates using the viewer's stated preferences for interests, distance and age, then selects the highest-ranked candidate.
5. The system confirms that the selection is still available and displays its preview: public display name, age, photo or placeholder, and available distance, bio excerpt and interest tags.

**MVP acceptance criteria**

- Blocks and hard preferences are enforced in both directions. Unknown data required by a hard preference prevents selection.
- Ranking is viewer-oriented and deterministic; equal scores use a stable profile-ID tie-breaker. Missing optional fields have defined fallbacks.
- The request produces one preview, an empty-feed result or an actionable error. The Swing interface remains responsive.
