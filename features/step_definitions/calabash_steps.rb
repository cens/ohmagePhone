require 'calabash-android/calabash_steps'

Given /^ohmage is in "(.*?)" campaign mode$/ do |mode|
  step %{I set boolean "key_single_campaign_mode" to "#{mode == "single"}" in the "org.ohmage_preferences" preferences file}
end