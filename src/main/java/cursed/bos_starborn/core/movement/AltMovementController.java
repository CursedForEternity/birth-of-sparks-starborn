package cursed.bos_starborn.core.movement;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public interface AltMovementController {
    void tickMovement(Player player, InputState input);

    record InputState(float forward, float strafe, boolean jump, boolean crouch) {}

    enum Mode implements AltMovementController {
        CAR {
            @Override
            public void tickMovement(Player player, InputState input) {
                // Turn yaw with strafe when moving forward/backward
                if (Math.abs(input.forward()) > 0.01f) {
                    float dir = Math.signum(input.forward());
                    player.setYRot(player.getYRot() + (-input.strafe() * 4.5f * dir));
                }

                Vec3 lookYaw = Vec3.directionFromRotation(0f, player.getYRot()).normalize();
                Vec3 vel = player.getDeltaMovement();

                double accel = input.forward() > 0 ? 0.085 : (input.forward() < 0 ? 0.045 : 0.0);
                double maxSpeed = input.forward() >= 0 ? 0.95 : 0.45;

                Vec3 nextHoriz = new Vec3(vel.x, 0, vel.z).add(lookYaw.scale(input.forward() * accel));
                if (nextHoriz.length() > maxSpeed) {
                    nextHoriz = nextHoriz.normalize().scale(maxSpeed);
                }

                // Light lateral grip so cars don't ice-skate forever
                Vec3 right = new Vec3(-lookYaw.z, 0, lookYaw.x);
                double lateral = nextHoriz.dot(right) * 0.25;
                nextHoriz = nextHoriz.subtract(right.scale(lateral));

                // Gravity + optional hop on jump
                double vy = input.jump() && player.onGround() ? 0.42 : vel.y;
                player.setDeltaMovement(nextHoriz.x, vy, nextHoriz.z);
            }
        },

        TANK {
            @Override
            public void tickMovement(Player player, InputState input) {
                // Tank steering: A/D rotates hull in place even while stationary
                player.setYRot(player.getYRot() + (-input.strafe() * 3.0f));

                Vec3 lookYaw = Vec3.directionFromRotation(0f, player.getYRot()).normalize();
                Vec3 vel = player.getDeltaMovement();

                double accel = 0.055;
                double maxSpeed = 0.55;

                Vec3 nextHoriz = new Vec3(vel.x, 0, vel.z).add(lookYaw.scale(input.forward() * accel));
                if (nextHoriz.length() > maxSpeed) {
                    nextHoriz = nextHoriz.normalize().scale(maxSpeed);
                }

                // Heavy tread damping
                nextHoriz = nextHoriz.scale(0.86);
                double vy = input.jump() && player.onGround() ? 0.35 : vel.y;

                player.setDeltaMovement(nextHoriz.x, vy, nextHoriz.z);
                if (player.onGround() && nextHoriz.lengthSqr() > 0.01) {
                    player.setMaxUpStep(1.1f); // Step up low blocks like a tank
                }
            }
        },

        JET {
            @Override
            public void tickMovement(Player player, InputState input) {
                player.fallDistance = 0f;

                // Roll/yaw turning + pitch from camera pitch
                player.setYRot(player.getYRot() + (-input.strafe() * 3.2f));
                float pitch = Mth.clamp(player.getXRot(), -65f, 65f);

                Vec3 forwardDir = Vec3.directionFromRotation(pitch, player.getYRot()).normalize();
                Vec3 vel = player.getDeltaMovement();

                // Jets maintain forward thrust; W boosts, S throttles down
                double throttle = input.forward() > 0 ? 1.35 : (input.forward() < 0 ? 0.65 : 1.0);
                double targetSpeed = 1.15 * throttle;
                double climb = input.jump() ? 0.25 : (input.crouch() ? -0.25 : 0.0);

                Vec3 targetVel = forwardDir.scale(targetSpeed).add(0, climb, 0);
                Vec3 blended = vel.lerp(targetVel, 0.14);

                player.setDeltaMovement(blended);
            }
        },

        HELI {
            @Override
            public void tickMovement(Player player, InputState input) {
                player.fallDistance = 0f;

                // Hovering yaw rotation with A/D
                player.setYRot(player.getYRot() + (-input.strafe() * 3.5f));

                Vec3 forwardYaw = Vec3.directionFromRotation(0f, player.getYRot()).normalize();
                Vec3 rightYaw = new Vec3(-forwardYaw.z, 0, forwardYaw.x);
                Vec3 vel = player.getDeltaMovement();

                // Horizontal cyclic movement (W/S forward/back, strafe slight side-slip)
                Vec3 desiredHoriz = forwardYaw.scale(input.forward() * 0.65)
                        .add(rightYaw.scale(input.strafe() * 0.20));

                // Collective vertical control: Space = ascend, Shift = descend, otherwise hover damp
                double vy;
                if (input.jump()) {
                    vy = 0.36;
                } else if (input.crouch()) {
                    vy = -0.32;
                } else {
                    vy = vel.y * 0.75; // Hover stabilization
                }

                Vec3 nextHoriz = new Vec3(vel.x, 0, vel.z).lerp(desiredHoriz, 0.12);
                player.setDeltaMovement(nextHoriz.x, vy, nextHoriz.z);
            }
        }
    }
}