"""Chitin family: grounded articulated legs, layered shell, bone jaws, amber eyes."""
import math
from engine import Model, Mat, Cube

SHELL = Mat('#302832', '#806958', 'chitin', 0.035, grad=0.18)
RIDGE = Mat('#685042', '#bd8c56', 'bone', 0.035, grad=0.14)
JOINT = Mat('#201920', '#51403c', 'noise', 0.04, grad=0.18)
FLESH = Mat('#78372e', '#b7623b', 'noise', 0.05)
TOOTH = Mat('#cabb94', '#736451', 'bone', 0.025)
EYE = Mat('#ffad3c', '#ffdc85', 'glow', 0.015, glow=True)
HOT = Mat('#df5431', '#ffd271', 'glow', 0.015, glow=True)


def eyes(model, queen):
    """Coordinates are local to each eye cluster, avoiding the old double offset."""
    for side, sx in (("l", -1), ("r", 1)):
        x = sx * (3.7 if queen else 2.5)
        model.part(f"eye_{side}", "head", pivot=(x, -1.1, -5.95 if queen else -4.55), cubes=[
            Cube((-0.6, -0.5, -0.5), (1.2, 1.8 if queen else 1.4, 1), JOINT),
            Cube((-0.42, -0.23, -0.71), (0.84, 0.6, 0.4), EYE),
            Cube((-0.3, 0.62, -0.63), (0.6, 0.44, 0.3), EYE)])


def jaws(model, queen):
    for side, sx in (("l", -1), ("r", 1)):
        model.part(f"mandible_{side}", "head",
                   pivot=(sx * (2.7 if queen else 1.8), 1.6, -5.7 if queen else -4.3),
                   rot=(18, -sx * 12, sx * 8), cubes=[
                       Cube((-0.7, -0.6, -2.8 if queen else -2.1), (1.4, 1.4, 3.6 if queen else 2.8), SHELL),
                       Cube((-0.45, 0.1, -4.5 if queen else -3.4), (0.9, 2.3 if queen else 1.7, 2.2), TOOTH),
                       Cube((-0.24, 1.65 if queen else 1.15, -4.8 if queen else -3.7), (0.48, 1, 0.8), TOOTH)])


def cheeks(model, queen):
    for side, sx in (("l", -1), ("r", 1)):
        model.part(f"cheek_{side}", "head", pivot=(sx * (3.0 if queen else 2.0), -0.1, -2.6 if queen else -2.0),
                   rot=(0, sx * 18, sx * 7), cubes=[
                       Cube((-0.8, -2.1 if queen else -1.7, -2.0), (1.6, 4.6 if queen else 3.6, 3.8 if queen else 3), SHELL)])


def legs(model, queen):
    """Solve hip height so each foot pivot rests at model y=24 (the ground)."""
    upper = 7.2 if queen else 4.2
    lower = 8.8 if queen else 6.2
    hip_x = 4.8 if queen else 2.65
    angle = 25 if queen else 20
    bend = 40 if queen else 48
    thick = 1.45 if queen else 0.85
    rows = (("front", -4, 32), ("mid_front", -1.2, 12), ("mid_back", 2.6, -12), ("rear", 6, -32))
    for side, sx in (("l", -1), ("r", 1)):
        for row, z, fan in rows:
            yaw = math.radians(sx * fan)
            a, b = math.radians(sx * angle), math.radians(sx * bend)
            drop = sx * upper * math.sin(a) * math.cos(yaw)
            drop += sx * lower * (math.sin(a) * math.cos(yaw) * math.cos(b) + math.cos(a) * math.sin(b))
            name = f"leg_{row}_{side}"
            model.part(name, pivot=(sx * hip_x, 24 - drop, z * (1 if queen else 0.65)),
                       rot=(0, sx * fan, sx * angle), cubes=[
                           Cube((-upper if sx < 0 else 0, -thick / 2, -thick / 2), (upper, thick, thick), SHELL),
                           Cube((sx * upper - 0.85, -0.9, -0.9), (1.7, 1.8, 1.8), RIDGE)])
            model.part(f"{name}_shin", name, pivot=(sx * upper, 0, 0), rot=(0, 0, sx * bend), cubes=[
                Cube((-lower if sx < 0 else 0, -thick * 0.32, -thick * 0.32), (lower, thick * 0.64, thick * 0.64), JOINT),
                Cube((-lower * 0.72 if sx < 0 else 0, -thick * 0.48, -thick * 0.48), (lower * 0.72, thick * 0.96, thick * 0.96), RIDGE)])
            model.part(f"{name}_foot", f"{name}_shin", pivot=(sx * lower, 0, 0),
                       rot=(0, 0, -sx * (angle + bend)), cubes=[
                           Cube((-0.6, -0.2, -0.9), (1.2, 0.4, 1.8), TOOTH)])


def crawler():
    model = Model("crawler", "CrawlerGeometry", 128, 128, seed=61)
    model.part("head", pivot=(0, 17, -3.4), cubes=[
        Cube((-2.5, -2.2, -4.4), (5, 4, 4.8), SHELL),
        Cube((-2.2, -2.7, -3.8), (4.4, 0.9, 3.8), RIDGE),
        Cube((-1.6, 0.8, -4.7), (3.2, 1.1, 2.2), FLESH)])
    eyes(model, False)
    jaws(model, False)
    cheeks(model, False)
    model.part("thorax", pivot=(0, 17, 0), cubes=[
        Cube((-3.2, -2.1, -3), (6.4, 4.2, 6.7), JOINT),
        Cube((-2.4, -2.8, -2), (4.8, 1, 4.6), SHELL)])
    model.part("abdomen", pivot=(0, 16.8, 3.2), cubes=[
        Cube((-3.9, -2.6, -0.5), (7.8, 5.2, 6), JOINT),
        Cube((-3.1, -2, 5.4), (6.2, 4, 3.7), JOINT)])
    for i, z in enumerate((0, 2.6, 5.2)):
        model.part(f"shell_plate_{i}", "abdomen", pivot=(0, -2.7, z), rot=(-6 + i * 5, 0, 0), cubes=[
            Cube((-3.8 + i * 0.3, -0.5, -0.5), (7.6 - i * 0.6, 1.2, 3.1), SHELL),
            Cube((-0.45, -0.8, -0.3), (0.9, 0.5, 2.7), RIDGE)])
    legs(model, False)
    return model


def crawler_queen():
    model = Model("crawler_queen", "CrawlerQueenGeometry", 256, 128, seed=67)
    model.part("head", pivot=(0, 12.6, -5.6), cubes=[
        Cube((-3.4, -2.8, -5.8), (6.8, 5.4, 6), SHELL),
        Cube((-3.1, -3.6, -5), (6.2, 1.2, 4.8), RIDGE),
        Cube((-2.4, 1.2, -6.3), (4.8, 1.5, 2.6), FLESH)])
    eyes(model, True)
    jaws(model, True)
    cheeks(model, True)
    model.part("crown", "head", pivot=(0, -3.1, -1.2), cubes=[
        Cube((-2.5, -1, -1), (5, 1.3, 2.6), SHELL)])
    for i in range(5):
        model.part(f"crown_spine_{i}", "crown", pivot=((i - 2) * 1.3, -0.3, 0),
                   rot=(-18, 0, (i - 2) * 13), decor=True, cubes=[
                       Cube((-0.45, -2.8 - (1 if i == 2 else 0), -0.4), (0.9, 3.5 if i == 2 else 2.8, 0.8), RIDGE),
                       Cube((-0.24, -3.7 - (1 if i == 2 else 0), -0.24), (0.48, 1.2, 0.48), TOOTH)])
    model.part("thorax", pivot=(0, 12.8, -1), cubes=[
        Cube((-5.2, -2.8, -4.5), (10.4, 5.6, 8.2), JOINT),
        Cube((-4.2, -4, -3.5), (8.4, 1.8, 6.5), SHELL)])
    model.part("abdomen", pivot=(0, 12.8, 4.5), cubes=[
        Cube((-5.4, -3.8, -1.1), (10.8, 7.6, 12.2), JOINT),
        Cube((-4.1, -2.8, 10.5), (8.2, 5.6, 5.2), JOINT),
        Cube((-3.2, 3.6, 1), (6.4, 1.7, 11), FLESH)])
    for side, sx in (("l", -1), ("r", 1)):
        model.part(f"shell_flap_{side}", "abdomen", pivot=(sx * 0.45, -4.8, 0),
                   rot=(0, 0, sx * 10))
        for i, z in enumerate((-0.8, 4.1, 9)):
            w = 6.4 - i * 0.85
            model.part(f"shell_panel_{side}_{i}", f"shell_flap_{side}", pivot=(0, i * 0.45, z),
                       rot=(-8 + i * 8, sx * (i - 1) * 3, sx * (4 + i * 2)), cubes=[
                           Cube((-w if sx < 0 else 0, -0.7, 0), (w, 1.6, 5.1), SHELL),
                           Cube((-w if sx < 0 else w - 0.55, -0.7, 0.2), (0.55, 0.8, 4.5), RIDGE)])
        model.part(f"acid_sac_{side}", "head", pivot=(sx * 3.3, 1.4, -1.2), cubes=[
            Cube((-0.7, -0.6, -1.6), (1.4, 1.4, 2.8), FLESH)])
    model.part("phase_two_fissures", "abdomen", cubes=[
        Cube((-0.24, -4.15, -0.3), (0.48, 0.4, 14), HOT),
        Cube((-4.7, -4.05, 4), (9.4, 0.36, 0.48), HOT),
        Cube((-4.1, -3.8, 9), (8.2, 0.36, 0.48), HOT)])
    model.part("phase_three_core", "abdomen", cubes=[
        Cube((-1.25, -4.1, 2), (2.5, 1.5, 8.5), HOT),
        Cube((-3.2, -3.9, 5), (6.4, 0.6, 0.55), HOT)])
    for i in range(4):
        model.part(f"rear_spine_{i}", "abdomen", pivot=(0, -4.2, 11 + i * 1.4),
                   rot=(18 + i * 8, 0, 0), decor=True, cubes=[
                       Cube((-0.45, -2.8, -0.45), (0.9, 3, 0.9), RIDGE)])
    legs(model, True)
    return model
