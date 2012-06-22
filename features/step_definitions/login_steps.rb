When /^I enter a developers credentials$/ do
  step %{I enter "ohmage.cameron" into "username field"}
  step %{I enter "Cameron.00" into "password field"}
end

When /^I enter bad credentials$/ do
  step %{I enter "bad_user" into "username field"}
  step %{I enter "invalid_pass" into "password field"}
end

When /^I choose the dev server$/ do
  step %{I press "choose server"}
  step %{I touch the "dev.andwellness.org" text}
end

When /^I log in with developers credentials$/ do
  step %{I enter a developers credentials}
  step %{I choose the dev server}
  step %{I press "Login"}
end

Given /^a user is logged in$/ do
  step %{the user "ohmage.cameron" exists with password "$2a$13$rhLkL/f7Oc4YlaUEKTZ/J.nyStF2JuDSh2y37jI/JjDyH7iMbDBJa"}
end

Given /^the user "([^\"]*)" exists with password "([^\"]*)"$/ do |username, password|
  step %{I set "username" to "#{username}" in the "org.ohmage_user_preferences" preferences file}
  step %{I set "hashedPassword" to "#{password}" in the "org.ohmage_user_preferences" preferences file}
  step %{I set "key_server_url" to "https://dev.andwellness.org/" in the "org.ohmage_preferences" preferences file}
end

Given /^no user is logged in$/ do
  step %{I remove "username" from the "org.ohmage_user_preferences" preferences file}
  step %{I remove "hashedPassword" from the "org.ohmage_user_preferences" preferences file}
end