
package org.ohmage;

import org.ohmage.Utilities.DataMapper;
import org.ohmage.adapters.ChartListAdapter.BubbleChartItem;
import org.ohmage.adapters.ChartListAdapter.HistogramChartItem;
import org.ohmage.adapters.SparklineAdapter.SparkLineChartItem;
import org.ohmage.charts.HistogramBase.CleanRenderer;
import org.ohmage.charts.HistogramBase.HistogramRenderer;

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

		public ExtraPromptData(String name, String s, int color, int min, int max,
				String label, int goodValue) {
			this(name, s, color, min, max, label, null, goodValue);
		}

		public ExtraPromptData(String name, String s, int color, int min, int max,
				String label, DataMapper mapper) {
			this(name, s, color, min, max, label, mapper, -1);
		}

		public ExtraPromptData(String name, String s, int color, int min, int max,
				String label) {
			this(name, s, color, min, max, label, null, -1);
		}

		public ExtraPromptData(String name, String s, int color, int min, int max,
				String label, DataMapper mapper, int goodValue) {
			shortName = name;
			SQL = s;
			colorId = color;
			this.min = min;
			this.max = max;
			this.label = label;
			this.mapper = mapper;
			this.goodValue = goodValue;
		}

		public HistogramChartItem toHistogramChartItem(double[] data, HistogramRenderer r) {
			return new HistogramChartItem(shortName, data, colorId, min, max, label, r, mapper);
		}

		public BubbleChartItem toBubbleChartItem(List<int[]> data, CleanRenderer r) {
			return new BubbleChartItem(shortName, data, colorId, min, max, label, goodValue, r);
		}

		public SparkLineChartItem toSparkLineChartItem(double[] data) {
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
	}

	private static final ExtraPromptData HOW_STRESSED = new ExtraPromptData("Stress Amount",
			"feltStress%", R.color.light_red, -1, 5, "times with low stress", 0);
	private static final ExtraPromptData FOOD_QUALITY = new ExtraPromptData("Food Quality",
			"foodQuality%", R.color.light_blue, -1, 2,
			"high quality meals eaten", 2);
	private static final ExtraPromptData FOOD_QUANTITY = new ExtraPromptData("Food Quantity",
			"foodHowMuch%", R.color.light_blue, -1, 2,
			"healthy size meals eaten", 1);
	private static final ExtraPromptData TIME_TO_YOURSELF = new ExtraPromptData(
			"Time For Self", "timeForYourself", R.color.light_purple, 0, 4,
			"hours", new DataMapper() {

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
			"didYouExercise", R.color.light_green, 0, 1, "times");

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
