Then /^I see all dashboard items$/ do
  step %{I see}, table(%{
    | Campaigns |
    | Surveys   |
    | Response  |
    | Upload    |
    | Profile   |
    | Mobility  |
    | Help      |
  })
end

Then /^I see all dashboard items except Campaigns$/ do
  step %{I see}, table(%{
    | Surveys  |
    | Response |
    | Upload   |
    | Profile  |
    | Mobility |
    | Help     |
  })
  step %{I should not see "Campaigns"}
end
