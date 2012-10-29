
Before do |scenario|
  @scenario_is_outline = (scenario.class == Cucumber::Ast::OutlineTable::ExampleRow) 
  if @scenario_is_outline 
    scenario = scenario.scenario_outline
  end

  StepCounter.step_index = 0
  StepCounter.step_line = scenario.raw_steps[StepCounter.step_index].line
end

AfterStep do |scenario|
  if @scenario_is_outline 
    scenario = scenario.scenario_outline 
  end

  #Handle multiline steps
  StepCounter.step_index = StepCounter.step_index + 1
  StepCounter.step_line = scenario.raw_steps[StepCounter.step_index].line unless scenario.raw_steps[StepCounter.step_index].nil?
end

StepCounter = Class.new
class << StepCounter
  @step_index = 0
  @step_line = 0
  attr_accessor :step_index, :step_line
end
