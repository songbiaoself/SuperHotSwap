package com.coderevolt;

/**
 * @author 公众号:codeRevolt
 */
public class HotswapException extends Exception{

  public HotswapException(String errorMsg) {
    super(errorMsg);
  }

  public HotswapException(String errorMsg, Throwable cause) {
    super(errorMsg, cause);
  }

}
