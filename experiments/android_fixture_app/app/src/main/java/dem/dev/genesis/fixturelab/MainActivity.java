package dem.dev.genesis.fixturelab;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Offline synthetic UI. No accounts, phones, network, payments or audio. */
public final class MainActivity extends Activity {
    private String selection = "";
    private TextView selected;
    private TextView result;
    private Button check;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(24 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);
        layout.setFitsSystemWindows(true);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);
        setContentView(scroll);

        TextView heading = new TextView(this);
        heading.setText("GENESIS — тестовый стенд");
        heading.setTextSize(24);
        layout.addView(heading);
        TextView description = new TextView(this);
        description.setText("Искусственные данные. Выберите сценарий и нажмите «Проверить».\n");
        layout.addView(description);
        addButton(layout, R.id.fixture_match, "Совпадение", v -> select("fixture-match"));
        addButton(layout, R.id.fixture_mismatch, "Несовпадение", v -> select("fixture-mismatch"));
        addButton(layout, R.id.fixture_missing, "Нет результата", v -> select("fixture-missing"));
        selected = new TextView(this);
        selected.setId(R.id.selected_fixture);
        layout.addView(selected);
        check = addButton(layout, R.id.check_fixture, "Проверить", v -> {
            String verdict;
            switch (selection) {
                case "fixture-match": verdict = "match"; break;
                case "fixture-mismatch": verdict = "mismatch"; break;
                case "fixture-missing": verdict = "not_found"; break;
                default: return;
            }
            result.setText(verdict);
        });
        result = new TextView(this);
        result.setId(R.id.fixture_result);
        result.setTextSize(22);
        result.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        layout.addView(result);
        addButton(layout, R.id.reset_fixture, "Сбросить", v -> select(""));
        select(state == null ? "" : state.getString("selection", ""));
    }

    private Button addButton(LinearLayout layout, int id, String text, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setId(id);
        button.setText(text);
        button.setOnClickListener(listener);
        layout.addView(button);
        return button;
    }

    private void select(String fixture) {
        if (!fixture.equals("fixture-match") && !fixture.equals("fixture-mismatch")
                && !fixture.equals("fixture-missing")) fixture = "";
        selection = fixture;
        selected.setText(selection.isEmpty() ? "Сценарий не выбран" : selection);
        result.setText("idle");
        check.setEnabled(!selection.isEmpty());
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("selection", selection);
        super.onSaveInstanceState(state);
    }
}
