package org.ohmage.prompt;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

public interface Displayable {

	View inflateView(Context context, ViewGroup parent);
}
