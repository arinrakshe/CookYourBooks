1. Design Rationale
Document (1/2 page):

Why did you choose this feature?
What user need does it address? (Reference a persona from GA0)
What alternatives did you consider?

User persona from GA0: Taylor Rodriguez, a college student who is renting her appartment and doesn't have constant access to school cafeteria. She wants to learn to cook moderately advanced dishes because in her culture cooking is valuable and she wants to learn to cook without going to culinary classes. She is capable to take a picture of a prepared dish and upload it anywhere. She is also capable to find a picture online, copy (or even save) and paste it wherever needed. Her goal is to broaden her standard cooking skills and learn to multitask and cook multiple different dishes at a time. Right now she is only able to cook simple dishes like eggs, grilled chicken with mashed potatoes, pasta marinara, or dishes that do not have multiple ingredients (like japanese rolls, beef wellington, or soups like borscht). She struggles with timings if she needs to prepare and interact with multiple ingredients at a time, and she also doesn't "feel" how much of each spices needs to be added, she must follow exact instructions or else gets lost or may add wrong quantity. She would always cook in her kitchen where she controls what she has, where everything is located, and how her stove works.

Why did we choose this feature + what user need  does it address: We chose this feature because students often stay late, on their phones, and we, as a college students who can relate to Taylor Rodriguez, definitely use and appreciate darkmode in pretty much any application. I, Konstantin, even downloaded an app that lowers my screen brightness below what I can do myself in settings. That feature helps avoid eye-damage for late night usage, but also some users prefer a darker layout, so this is also a cosmetic capability.

Alternatives we considered: We considered changing the general layout color to something less bright, more universal, but it doesn't look optimal, and ultimately a capability to switch between two versions is better than having a middle-ground version.


2. Design Artifacts (actually I'll rewrite entire answer, that's my part)
Show your design evolution:

Version 1: Initial wireframe/mockup
Version 2+: At least one iteration with documented changes
Rationale: Why did you change the design? What feedback prompted it?
Photos of whiteboard sketches are fine. The goal is showing iteration, not polish.

TODO: [Insert pictures]

We first didn't think much about button cosmetic, since we focused first on implementing functionality. During functionality, we played with some different colors until choosing final version. 

Regarding the button/slider, first iteration was just a clikable text. We implemented it first just to have a capability to switch between the two modes, but we knew this isn't perfect, and we would change it later. On our first iteration, it didn't seem like it's an interactive text to the user, so we then made a sliding button. During the making of it, we tried to have a text within a button, but it looked ugly. We still knew that a button can't be completely empty, it has to contain something that a user immediately understand what the button does. We decided to use an icon of a sun and a moon, we believe this is pretty intuitive to the user, and also looks nice and compact.


3. Implementation Journal
Document your implementation process:

Git history: Show incremental progress (not one giant commit at the end)
PR history: Link to PRs with meaningful review comments
Decision log: At least one documented technical decision with alternatives considered

Git history: we made sure we take incremental steps in the implementation of the featuer.

TODO: PR history: (provide link(s) to PRs with meaningful review comments)

TODO: Decision log: (At least one documented technical decision with alternatives considered)

4. Testing & Quality
TODO: Unit tests for the feature
TODO: Brief accessibility check: Does it support keyboard navigation?
Known limitations: There are no known limitations.
TODO: DOESN'T WORK KEY NAVIGATION

5. Feature Summary
Screenshots: 2-3 screenshots showing the feature in action
Integration notes: How does this feature connect to the rest of the app?
Status: What's complete, what's in progress, what's known to be broken?

Screenshots:
![lightmode](screenshots/lightmode.png)
![darkmode](screenshots/darkmode.png)

Integration notes: The feature extends usability of the app and allows people to use the app when it's dark in the room. The feature also extends customizability for users, adds capability to choose a layout that is more pleasing to the eye (regardless if it's dark or not in the room)

Status: The feature is fully completed, polished, and doesn't have bugs.
