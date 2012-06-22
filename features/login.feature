Feature: Login feature
  In order to know who is using making responses,
  As the system
  I need users to log in to the app

  Scenario: As a valid user I can log into my app
    Given no user is logged in
    When I start the app
    And I log in with developers credentials
	  Then I see "Authenticating with"
	  And I wait for the "DashboardActivity" screen to appear

	Scenario: As an invalid user I can't log into my app
		Given no user is logged in
		When I start the app
    When I enter bad credentials
		And I press "Login"
		Then I wait to see "Login Error"

  @no_network
	Scenario: An error is shown if there is no network connection
		Given no user is logged in
		And there is no network connectivity
		When I start the app
    And I log in with developers credentials
		Then I wait to see "Network Error"