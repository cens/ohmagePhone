
package org.ohmage;

import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.ohmage.Utilities.DataMapper;
import org.ohmage.adapters.ChartListAdapter.BubbleChartItem;
import org.ohmage.adapters.ChartListAdapter.HistogramChartItem;
import org.ohmage.adapters.SparklineAdapter.SparkLineChartItem;
import org.ohmage.charts.HistogramBase.CleanRenderer;
import org.ohmage.charts.HistogramBase.HistogramRenderer;
import org.ohmage.loader.PromptFeedbackLoader.FeedbackItem;

import java.util.List;

// Configurations for the feedback prompts
public class NIHConfig {

	public static class ExtraPromptData {
		public final String shortName;
		public final String SQL;
		private final int colorId;
		private final int min;
		private final int max;
		private final String label;
		private final DataMapper mapper;
		private final int goodValue;
		public final String[] valueLabels;

		public ExtraPromptData(String name, String s, int color, int min, int max,
				String label, String[] valueLabels, int goodValue) {
			this(name, s, color, min, max, label, valueLabels, null, goodValue);
		}

		public ExtraPromptData(String name, String s, int color, int min, int max,
				String label, String[] valueLabels, DataMapper mapper) {
			this(name, s, color, min, max, label, valueLabels, mapper, -1);
		}

		public ExtraPromptData(String name, String s, int color, int min, int max,
				String label, String[] valueLabels) {
			this(name, s, color, min, max, label, valueLabels, null, -1);
		}

		public ExtraPromptData(String name, String s, int color, int min, int max,
				String label, String[] valueLabels, DataMapper mapper, int goodValue) {
			shortName = name;
			SQL = s;
			colorId = color;
			this.min = min;
			this.max = max;
			this.label = label;
			this.mapper = mapper;
			this.goodValue = goodValue;
			this.valueLabels = valueLabels;
		}

		public HistogramChartItem toHistogramChartItem(List<FeedbackItem> data, HistogramRenderer r) {
			addLabels(r);
			r.setShowLabels(true);
			return new HistogramChartItem(shortName, data, colorId, min, max, label, r, mapper);
		}

		public BubbleChartItem toBubbleChartItem(List<FeedbackItem> data, CleanRenderer r) {
			addLabels(r);
			r.setShowLabels(true);
			r.setShowGrid(true);
			return new BubbleChartItem(shortName, data, colorId, min - 1, max, label, goodValue, r);
		}

		public SparkLineChartItem toSparkLineChartItem(List<FeedbackItem> data) {
			return new SparkLineChartItem(shortName, data, colorId, min, max);
		}

		/**
		 * Returns the range which will be used when making the bubble charts.
		 * It is calculated as the max - min
		 * 
		 * @return
		 */
		public int getRange() {
			return max - min;
		}

		public DataMapper getMapper() {
			if (mapper == null)
				return Utilities.linearDataMapper;
			return mapper;
		}

		public int getColor() {
			return colorId;
		}

		public double getMin() {
			return min;
		}

		public double getMax() {
			return max;
		}

		public void addLabels(XYMultipleSeriesRenderer r) {
			int i=Integer.valueOf(min);
			r.addYTextLabel(i-1, "");
			for(String label : valueLabels) {
				r.addYTextLabel(i++, label);
			}
			r.setYLabels(valueLabels.length);
		}
	}

	private static final ExtraPromptData HOW_STRESSED = new ExtraPromptData("Stress Amount",
			"feltStress%", R.color.light_red, 0, 3, "Stress Level", new String[] {
					"None", "Low", "Med", "High"
			}, 0);
	private static final ExtraPromptData FOOD_QUALITY = new ExtraPromptData("Food Quality",
			"foodQuality%", R.color.light_blue, 0, 2,
			"Meal Quality", new String[] {
					"Low", "Med", "High"
			}, 2);
	private static final ExtraPromptData FOOD_QUANTITY = new ExtraPromptData("Food Quantity",
			"foodHowMuch%", R.color.light_blue, 0, 2,
			"Meal Size", new String[] {
					"Small", "Just Right", "Large"
			}, 1);
	private static final ExtraPromptData TIME_TO_YOURSELF = new ExtraPromptData(
			"Time For Self", "timeForYourself", R.color.light_purple, 0, 4,
			"Hours", new String[] {
					"0", "<.5", "< 1", "> 1", "> 2"
			}, new DataMapper() {

				@Override
				public double translate(double d) {
					switch (Double.valueOf(d).intValue()) {
						case 0:
							return 0.0;
						case 1:
							return 0.5;
						case 2:
							return 1;
						case 3:
							return 2;
						case 4:
							return 4;
						default:
							return d;
					}
				}
			});

	private static final ExtraPromptData DID_EXERCISE = new ExtraPromptData("Did Exercise",
			"didYouExercise", R.color.light_green, 0, 1, null, new String[] {
					"No", "Yes"
			});

	public static final String[] PROMPT_LIST = new String[] {
			NIHConfig.SQL.FOOD_QUALITY_ID,
			NIHConfig.SQL.FOOD_QUANTITY_ID,
			NIHConfig.SQL.DID_EXERCISE_ID,
			NIHConfig.SQL.HOW_STRESSED_ID,
			NIHConfig.SQL.TIME_TO_YOURSELF_ID
	};

	public static ExtraPromptData getExtraPromptData(String promptId) {
		if (promptId.startsWith(Prompt.HOW_STRESSED_ID))
			return HOW_STRESSED;
		else if (promptId.startsWith(Prompt.FOOD_QUALITY_ID))
			return FOOD_QUALITY;
		else if (promptId.startsWith(Prompt.FOOD_QUANTITY_ID))
			return FOOD_QUANTITY;
		else if (promptId.startsWith(Prompt.TIME_TO_YOURSELF_ID))
			return TIME_TO_YOURSELF;
		else if (promptId.startsWith(Prompt.DID_EXERCISE_ID))
			return DID_EXERCISE;
		return null;
	}

	public static class Surveys {
		public static final String FOOD_BUTTON = "foodButton";
		public static final String STRESS_BUTTON = "stressButton";
		public static final String MORNING = "Morning";
		public static final String MIDDAY = "Mid Day";
		public static final String LATEAFTERNOON = "Late Afternoon";
		public static final String BEDTIME = "Bedtime";
	}

	public static class Prompt {
		public static final String HOW_STRESSED_ID = "feltStress";
		public static final String FOOD_QUALITY_ID = "foodQuality";
		public static final String FOOD_QUANTITY_ID = "foodHowMuch";
		public static final String TIME_TO_YOURSELF_ID = "timeForYourself";
		public static final String DID_EXERCISE_ID = "didYouExercise";
	}

	public static class SQL {
		public static final String HOW_STRESSED_ID = "feltStress%";
		public static final String FOOD_QUALITY_ID = "foodQuality%";
		public static final String FOOD_QUANTITY_ID = "foodHowMuch%";
		public static final String TIME_TO_YOURSELF_ID = "timeForYourself";
		public static final String DID_EXERCISE_ID = "didYouExercise";
	}

	public static String getPrompt(String promptId) {
		if (promptId.startsWith(Prompt.HOW_STRESSED_ID))
			return Prompt.HOW_STRESSED_ID;
		else if (promptId.startsWith(Prompt.FOOD_QUALITY_ID))
			return Prompt.FOOD_QUALITY_ID;
		else if (promptId.startsWith(Prompt.FOOD_QUANTITY_ID))
			return Prompt.FOOD_QUANTITY_ID;
		else if (promptId.startsWith(Prompt.TIME_TO_YOURSELF_ID))
			return Prompt.TIME_TO_YOURSELF_ID;
		else if (promptId.startsWith(Prompt.DID_EXERCISE_ID))
			return Prompt.DID_EXERCISE_ID;
		return null;
	}
}
