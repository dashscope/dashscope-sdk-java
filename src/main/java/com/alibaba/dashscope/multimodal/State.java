package com.alibaba.dashscope.multimodal;

import lombok.Getter;

/**
 * author songsong.shao
 * date 2025/4/27
 */
@Getter
public class State {
  @Getter
  public enum DialogState {
    /** 对话状态枚举类，定义了对话机器人可能处于的不同状态。 */
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
   * 状态机类，用于管理机器人的状态转换。 -- GETTER -- 获取当前状态。
   *
   * return 当前状态。
   */
  private DialogState currentState;

  /** 初始化状态机时设置初始状态为IDLE。 */
  public State() {
    this.currentState = DialogState.IDLE;
  }
}
