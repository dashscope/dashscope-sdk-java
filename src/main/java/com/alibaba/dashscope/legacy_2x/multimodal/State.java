package com.alibaba.dashscope.legacy_2x.multimodal;

import lombok.Getter;

/** author songsong.shao date 2025/4/27 */
@Getter
public class State {
  @Getter
  public enum DialogState {
    /** Dialog states that a chatbot can be in. */
    IDLE("Idle"),
    LISTENING("Listening"),
    THINKING("Thinking"),
    RESPONDING("Responding");

    private final String value;

    DialogState(String value) {
      this.value = value;
    }
  }

  /**
   * State machine for managing robot state transitions. -- GETTER -- Gets the current state.
   *
   * <p>return The current state.
   */
  private DialogState currentState;

  /** Sets the initial state to IDLE when initializing the state machine. */
  public State() {
    this.currentState = DialogState.IDLE;
  }
}
