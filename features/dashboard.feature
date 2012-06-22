Feature: Dashboard feature
  In order to access certain features of the app,
  As the user,
  I need to see the correct items on the dashboard and be able
  to click on them to lead me to the correct activities

  Scenario: All the correct items are shown on the dashboard
    Given a user is logged in
    And ohmage is in "multi" campaign mode
    When I start the app
    Then I wait for the "DashboardActivity" screen to appear
    And I see all dashboard items

  Scenario: Campaigns are not shown on the dashboard for single campaign mode
    Given a user is logged in
    And ohmage is in "single" campaign mode
    When I start the app
    Then I wait for the "DashboardActivity" screen to appear
    And I see all dashboard items except Campaigns

  @smoke
  Scenario Outline: Clicking on dashboard icons should start the correct activity
    Given a user is logged in
    And ohmage is in "multi" campaign mode
    When I start the app
    And I touch the "<text>" text
    Then I wait for the "<expected activity>" screen to appear

      Examples:
      | text      | expected activity       |
      | Campaigns | CampaignListActivity    |
      | Surveys   | SurveyListActivity      |
      | Response  | ResponseHistoryActivity |
      | Upload    | UploadQueueActivity     |
      | Profile   | ProfileActivity         |
      | Mobility  | MobilityActivity        |
      | Help      | HelpActivity            |

  @smoke
  Scenario: Clicking on Menu should lead to settings
    Given a user is logged in
    When I start the app
    And I press the menu key
    And I touch the "Settings" text
    Then I wait for the "OhmagePreferenceActivity" screen to appear