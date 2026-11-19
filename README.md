# Team Project

The readme should include information such as:
- a summary of what your application is all about
- a list of the user stories, along with who is responsible for each one
- information about the API(s) that your project uses 
- screenshots or animations demonstrating current functionality

### User stories
| ID | User story | Scope | Lead |
| --- | --- | --- | --- |
| MM-1 |As a user, I want to be able to register and create a new account Using my own username, password, email, and phone number, so that I can use the dating app with a private account| MVP a Create Account screen that takes in username, password, email, and phone as input and stores it in a database| Simba Zhou| 
| MM-2 |As a user, I want to be able to login to the account that I have created so that I can access the services that the dating app provides| MVP a Login screen that takes in username and password as input and verifies if the password and username match the one stored in the database| Simba Zhou| 
| MM-3 |As a user, I want to be able to retrieve access to my account if I forget my login credentials, so that I can still access my account| MVP a Reset Password screen that takes in username and phone number as input, checks if they match in the database; and if they do, allow the user to input a new password | Simba Zhou| 

### Use case: Create a new Profile

**Related story:** MM-1

**Primary actor:** User without an associated profile

**Lead:** Simba Zhou

**Preconditions:** The User inputs a valid non-empty username, password, email, and phone number, and the username, email, and phone number is not already recorded in the database

**Trigger:** The User clicks on the create profile button

**Success outcome:** The Profile gets saved in the database and a userid is assigned to the Profile, and the User gets a prompt that their Profile has been created

**Main success scenario**
1. The User inputs the fields and submits them through the create profile button
2. The system checks if the fields are empty
3. The system checks if the fields are invalid i.e. letters in phone number
4. The system checks if username, email, or phone number is already in saved in another profile in the databse
5. The system confirms that this is a valid Profile, saves it to the database, and informs the user that their profile has been created

**MVP acceptance criteria**

- All Profiles has distinct userIDs, as well as username, Email, and Phone number
- Profiles can be created and the user can login with their username and password
- if invalid fields are passed, they are all caught by exceptions or statements in the code

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
