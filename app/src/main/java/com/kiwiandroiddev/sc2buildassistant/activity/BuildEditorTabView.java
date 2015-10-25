package com.kiwiandroiddev.sc2buildassistant.activity;

/**
 * Created by matt on 19/10/15.
 */
public interface BuildEditorTabView {
    boolean requestsAddButton();
    void onAddButtonClicked();

    BuildEditorTabView DefaultBuildEditorTabView = new BuildEditorTabView() {
        @Override
        public boolean requestsAddButton() {
            return false;
        }

        @Override
        public void onAddButtonClicked() {}
    };
}
