import { describe, expect, it } from "vitest";
import { allowedTransitions } from "./stateMachine";

describe("allowedTransitions", () => {
  it("matches state-machine spec for OPEN", () => {
    expect(allowedTransitions("OPEN")).toEqual(["IN_PROGRESS", "CANCELLED"]);
  });

  it("has no outbound transitions from CLOSED", () => {
    expect(allowedTransitions("CLOSED")).toEqual([]);
  });

  it("allows RESOLVED to CLOSED only", () => {
    expect(allowedTransitions("RESOLVED")).toEqual(["CLOSED"]);
  });
});
