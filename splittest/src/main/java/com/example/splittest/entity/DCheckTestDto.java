package com.example.splittest.entity;

import authserver.common.CheckTestDto;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import lombok.Data;

@Data
public class DCheckTestDto implements Delayed {

  private final CheckTestDto dto;
  private final long time;

  public DCheckTestDto(CheckTestDto dto, long delayTime) {
    this.dto = dto;
    this.time = System.currentTimeMillis() + delayTime;
  }

  @Override
  public long getDelay(TimeUnit unit) {
    long diff = time - System.currentTimeMillis();
    return unit.convert(diff, TimeUnit.MILLISECONDS);
  }

  @Override
  public int compareTo(Delayed obj) {
    if (this.time < ((DCheckTestDto) obj).time) {
      return -1;
    }
    if (this.time > ((DCheckTestDto) obj).time) {
      return 1;
    }
    return 0;
  }
}
