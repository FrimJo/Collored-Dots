package com.fredrikux.unitedcolors.utils;


public interface IActionListener {
    void onActionPerformed(final ActionEvent event);

    class ActionEvent {

        public final int action;
        public final Object source;
        public final String message;

        public ActionEvent(final int action, final Object source,
                           final String message){
            this.action = action;
            this.source = source;
            this.message = message;
        }
    }
}
