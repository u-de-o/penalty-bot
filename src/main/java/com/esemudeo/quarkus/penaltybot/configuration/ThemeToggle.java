package com.esemudeo.quarkus.penaltybot.configuration;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

/**
 * Toggles the Lumo dark theme and persists the choice in the browser's localStorage so
 * it applies across every page and full page reload (including the OAuth round trip).
 * When the user has not chosen yet, it follows the operating system's color-scheme.
 */
public class ThemeToggle extends Button {

	private static final String DARK_THEME = "dark";
	private static final String LIGHT_VALUE = "light";
	private static final String STORAGE_KEY = "penalty-bot-theme";
	private static final String DARK_MODE_LABEL = "To the dark room";
	private static final String LIGHT_MODE_LABEL = "Back to the light";

	// Reads the stored preference, falling back to the OS color-scheme preference.
	private static final String JS_RESOLVE_THEME =
			"const stored = localStorage.getItem($0);"
			+ "if (stored === 'dark') { return true; }"
			+ "if (stored === 'light') { return false; }"
			+ "return window.matchMedia('(prefers-color-scheme:dark)').matches;";

	private static final String JS_STORE_THEME = "localStorage.setItem($0, $1)";

	private final boolean iconOnly;
	private boolean dark;

	public ThemeToggle() {
		this(false);
	}

	/** When {@code iconOnly} is true, no text label is shown (only the icon and a native tooltip). */
	public ThemeToggle(boolean iconOnly) {
		this.iconOnly = iconOnly;
		if (iconOnly) {
			getElement().setAttribute("title", "Toggle dark mode");
		}
		addClickListener(e -> toggle());
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		UI.getCurrent().getPage().executeJs(JS_RESOLVE_THEME, STORAGE_KEY).then(Boolean.class, this::applyTheme);
	}

	private void toggle() {
		applyTheme(!dark);
		UI.getCurrent().getPage().executeJs(JS_STORE_THEME, STORAGE_KEY, dark ? DARK_THEME : LIGHT_VALUE);
	}

	private void applyTheme(boolean useDark) {
		this.dark = useDark;
		var themeList = UI.getCurrent().getElement().getThemeList();
		if (useDark) {
			themeList.add(DARK_THEME);
		} else {
			themeList.remove(DARK_THEME);
		}
		if (!iconOnly) {
			setText(useDark ? LIGHT_MODE_LABEL : DARK_MODE_LABEL);
		}
		setIcon(new Icon(useDark ? VaadinIcon.SUN_O : VaadinIcon.MOON));
	}
}
