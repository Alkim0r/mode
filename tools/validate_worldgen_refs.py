#!/usr/bin/env python3
"""Validate Regnum's data-pack references used by jigsaw world generation."""

from __future__ import annotations

import json
import sys
import io
import zipfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from tools.mc_convert import tnbt


DATA = ROOT / "src" / "main" / "resources" / "data"
errors: list[str] = []


def read_json(path: Path) -> dict:
    try:
        value = json.loads(path.read_text(encoding="utf-8-sig"))
    except (OSError, json.JSONDecodeError) as exc:
        errors.append(f"invalid JSON {path.relative_to(DATA)}: {exc}")
        return {}
    if not isinstance(value, dict):
        errors.append(f"expected JSON object in {path.relative_to(DATA)}")
        return {}
    return value


def resource_id(path: Path, kind: str, namespace_root: Path) -> str:
    relative = path.relative_to(namespace_root / "worldgen" / kind).with_suffix("")
    return f"{namespace_root.name}:{relative.as_posix()}"


def referenced_path(identifier: str, kind: str, extension: str) -> Path | None:
    if ":" not in identifier:
        return None
    namespace, path = identifier.split(":", 1)
    if not namespace or not path or path.startswith("/") or ".." in Path(path).parts:
        return None
    return DATA / namespace / kind / f"{path}.{extension}"


def check_json_tree(root: Path) -> None:
    for path in root.rglob("*.json"):
        read_json(path)


def check_jigsaw_targets(tag: tuple, owner: Path, pool_ids: set[str], count: list[int]) -> None:
    tag_type, value = tag
    if tag_type == 10:
        for key, child in value.items():
            child_type, child_value = child
            if key in ("pool", "target_pool") and child_type == 8 and isinstance(child_value, str):
                count[0] += 1
                if child_value.startswith("regnum:") and child_value not in pool_ids:
                    errors.append(f"missing NBT jigsaw target pool {child_value} in {owner.relative_to(DATA)}")
            check_jigsaw_targets(child, owner, pool_ids, count)
    elif tag_type == 9:
        element_type, elements = value
        for element in elements:
            check_jigsaw_targets((element_type, element), owner, pool_ids, count)


def check_loot_table_refs(tag: tuple, owner: Path, count: list[int]) -> None:
    """Check project loot-table references embedded in NBT block entities."""
    tag_type, value = tag
    if tag_type == 10:
        for key, child in value.items():
            child_type, child_value = child
            if key in ("LootTable", "loot_table") and child_type == 8 and isinstance(child_value, str):
                if child_value.startswith("regnum:"):
                    count[0] += 1
                    target = referenced_path(child_value, "loot_table", "json")
                    if target is None or not target.is_file():
                        errors.append(f"missing NBT loot table {child_value} in {owner.relative_to(DATA)}")
            check_loot_table_refs(child, owner, count)
    elif tag_type == 9:
        element_type, elements = value
        for element in elements:
            check_loot_table_refs((element_type, element), owner, count)


def collect_jigsaw_connectors(tag: tuple, result: list[dict[str, str]]) -> None:
    """Collect socket names from jigsaw block entities in an NBT structure."""
    tag_type, value = tag
    if tag_type == 10:
        entity_id = value.get("id", (0, ""))
        if entity_id == (8, "minecraft:jigsaw"):
            result.append({
                key: value.get(key, (8, ""))[1]
                for key in ("name", "target", "pool")
            })
        for child in value.values():
            check_type, _ = child
            if check_type in (9, 10):
                collect_jigsaw_connectors(child, result)
    elif tag_type == 9:
        element_type, elements = value
        if element_type in (9, 10):
            for element in elements:
                collect_jigsaw_connectors((element_type, element), result)


def vanilla_structure_resources(server_jar: Path) -> set[str]:
    """Read vanilla structure resource paths from a server jar or bundled server jar."""
    with zipfile.ZipFile(server_jar) as archive:
        names = set(archive.namelist())
        if any(name.startswith("data/minecraft/structure/") for name in names):
            return {name for name in names if name.startswith("data/minecraft/structure/") and name.endswith(".nbt")}
        nested = next((name for name in names if name.startswith("META-INF/versions/") and name.endswith("/server-1.21.1.jar")), None)
        if nested is None:
            raise ValueError(f"no vanilla 1.21.1 server jar found in {server_jar}")
        with zipfile.ZipFile(io.BytesIO(archive.read(nested))) as server:
            return {name for name in server.namelist() if name.startswith("data/minecraft/structure/") and name.endswith(".nbt")}


def main() -> int:
    vanilla_jar_arg = next((arg.split("=", 1)[1] for arg in sys.argv[1:] if arg.startswith("--vanilla-server-jar=")), None)
    vanilla_resources: set[str] | None = None
    if vanilla_jar_arg:
        try:
            vanilla_resources = vanilla_structure_resources(Path(vanilla_jar_arg))
        except (OSError, zipfile.BadZipFile, ValueError) as exc:
            errors.append(f"cannot inspect vanilla server jar: {exc}")
    pool_ids: set[str] = set()
    processor_ids: set[str] = set()
    structures: set[str] = set()
    placed_feature_ids: set[str] = set()
    placed_features: list[tuple[Path, dict]] = []
    biome_modifiers: list[tuple[Path, dict]] = []
    structure_tag_ids: set[str] = set()
    structure_tag_files: list[tuple[Path, dict]] = []
    placed_structures: set[str] = set()
    structure_set_salts: dict[int, Path] = {}
    structure_set_count = 0
    templates: set[Path] = set()
    pools: list[tuple[Path, dict]] = []
    pool_id_by_path: dict[Path, str] = {}
    pools_with_external_templates: set[str] = set()
    structure_start_pools: list[tuple[Path, str, str]] = []
    template_owners: dict[Path, set[str]] = {}

    for namespace_root in DATA.iterdir():
        if not namespace_root.is_dir():
            continue
        pool_root = namespace_root / "worldgen" / "template_pool"
        processor_root = namespace_root / "worldgen" / "processor_list"
        structure_root = namespace_root / "worldgen" / "structure"
        placed_feature_root = namespace_root / "worldgen" / "placed_feature"
        biome_modifier_root = namespace_root / "neoforge" / "biome_modifier"
        for path in pool_root.rglob("*.json") if pool_root.exists() else ():
            pool_ids.add(resource_id(path, "template_pool", namespace_root))
            pools.append((path, read_json(path)))
            pool_id_by_path[path] = resource_id(path, "template_pool", namespace_root)
        for path in processor_root.rglob("*.json") if processor_root.exists() else ():
            processor_ids.add(resource_id(path, "processor_list", namespace_root))
            read_json(path)
        for path in structure_root.rglob("*.json") if structure_root.exists() else ():
            structures.add(resource_id(path, "structure", namespace_root))
            read_json(path)
        for path in placed_feature_root.rglob("*.json") if placed_feature_root.exists() else ():
            placed_feature_ids.add(resource_id(path, "placed_feature", namespace_root))
            placed_features.append((path, read_json(path)))
        for path in biome_modifier_root.rglob("*.json") if biome_modifier_root.exists() else ():
            biome_modifiers.append((path, read_json(path)))
        for kind in ("structure_set",):
            check_json_tree(namespace_root / "worldgen" / kind) if (namespace_root / "worldgen" / kind).exists() else None
        tag_root = namespace_root / "tags" / "worldgen" / "structure"
        for path in tag_root.rglob("*.json") if tag_root.exists() else ():
            relative = path.relative_to(tag_root).with_suffix("")
            structure_tag_ids.add(f"{namespace_root.name}:{relative.as_posix()}")
            structure_tag_files.append((path, read_json(path)))

    for namespace_root in DATA.iterdir():
        if not namespace_root.is_dir():
            continue
        for path in (namespace_root / "worldgen" / "structure").rglob("*.json") if (namespace_root / "worldgen" / "structure").exists() else ():
            structure = read_json(path)
            start_pool = structure.get("start_pool")
            start_name = structure.get("start_jigsaw_name", "")
            if isinstance(start_pool, str):
                structure_start_pools.append((path, start_pool, start_name if isinstance(start_name, str) else ""))
            if isinstance(start_pool, str) and start_pool.startswith("regnum:") and start_pool not in pool_ids:
                errors.append(f"missing start pool {start_pool} in {path.relative_to(DATA)}")
        for path in (namespace_root / "worldgen" / "structure_set").rglob("*.json") if (namespace_root / "worldgen" / "structure_set").exists() else ():
            structure_set = read_json(path)
            structure_set_count += 1
            entries = structure_set.get("structures")
            if not isinstance(entries, list) or not entries:
                errors.append(f"structure_set has no structures: {path.relative_to(DATA)}")
                entries = []
            for index, entry in enumerate(entries):
                if not isinstance(entry, dict):
                    errors.append(f"invalid structure entry {index} in {path.relative_to(DATA)}")
                    continue
                identifier = entry.get("structure", "") if isinstance(entry, dict) else ""
                weight = entry.get("weight", 1)
                if isinstance(weight, bool) or not isinstance(weight, int) or weight <= 0:
                    errors.append(f"invalid structure weight {weight!r} in {path.relative_to(DATA)}[{index}]")
                if isinstance(identifier, str) and identifier.startswith("regnum:"):
                    if identifier not in structures:
                        errors.append(f"missing structure {identifier} in {path.relative_to(DATA)}")
                    else:
                        placed_structures.add(identifier)

            placement = structure_set.get("placement")
            if not isinstance(placement, dict):
                errors.append(f"structure_set has no placement object: {path.relative_to(DATA)}")
                continue
            if placement.get("type") == "minecraft:random_spread":
                spacing = placement.get("spacing")
                separation = placement.get("separation")
                salt = placement.get("salt")
                if isinstance(spacing, bool) or not isinstance(spacing, int) or spacing <= 0:
                    errors.append(f"invalid random_spread spacing in {path.relative_to(DATA)}")
                if isinstance(separation, bool) or not isinstance(separation, int) or separation < 0:
                    errors.append(f"invalid random_spread separation in {path.relative_to(DATA)}")
                elif isinstance(spacing, int) and not isinstance(spacing, bool) and spacing > 0 and separation >= spacing:
                    errors.append(f"random_spread separation must be less than spacing in {path.relative_to(DATA)}")
                if isinstance(salt, bool) or not isinstance(salt, int):
                    errors.append(f"invalid random_spread salt in {path.relative_to(DATA)}")
                elif salt in structure_set_salts:
                    errors.append(
                        f"duplicate random_spread salt {salt} in {path.relative_to(DATA)} "
                        f"and {structure_set_salts[salt].relative_to(DATA)}"
                    )
                else:
                    structure_set_salts[salt] = path

    for identifier in sorted(structures - placed_structures):
        errors.append(f"structure has no structure_set placement: {identifier}")

    for path, placed_feature in placed_features:
        feature = placed_feature.get("feature")
        if isinstance(feature, str) and feature.startswith("regnum:"):
            target = referenced_path(feature, "worldgen/configured_feature", "json")
            if target is None or not target.is_file():
                errors.append(f"missing configured feature {feature} in {path.relative_to(DATA)}")

    for path, biome_modifier in biome_modifiers:
        if biome_modifier.get("type") != "neoforge:add_features":
            continue
        features = biome_modifier.get("features", [])
        if isinstance(features, str):
            features = [features]
        if not isinstance(features, list):
            errors.append(f"invalid add_features list in {path.relative_to(DATA)}")
            continue
        for identifier in features:
            if isinstance(identifier, str) and identifier.startswith("regnum:") and identifier not in placed_feature_ids:
                errors.append(f"missing placed feature {identifier} in {path.relative_to(DATA)}")

    for path, structure_tag in structure_tag_files:
        for entry in structure_tag.get("values", []):
            identifier = entry if isinstance(entry, str) else entry.get("id", "") if isinstance(entry, dict) else ""
            if not isinstance(identifier, str):
                continue
            if identifier.startswith("#regnum:") and identifier[1:] not in structure_tag_ids:
                errors.append(f"missing nested structure tag {identifier} in {path.relative_to(DATA)}")
            elif identifier.startswith("regnum:") and identifier not in structures:
                errors.append(f"missing structure {identifier} in {path.relative_to(DATA)}")

    for path, pool in pools:
        fallback = pool.get("fallback")
        if isinstance(fallback, str) and fallback.startswith("regnum:") and fallback not in pool_ids:
            errors.append(f"missing fallback pool {fallback} in {path.relative_to(DATA)}")
        for index, entry in enumerate(pool.get("elements", [])):
            element = entry.get("element", {}) if isinstance(entry, dict) else {}
            if not isinstance(element, dict):
                errors.append(f"invalid element {index} in {path.relative_to(DATA)}")
                continue
            if element.get("element_type") == "minecraft:single_pool_element":
                location = element.get("location", "")
                target = referenced_path(location, "structure", "nbt") if isinstance(location, str) else None
                if isinstance(location, str) and location.startswith("regnum:") and (target is None or not target.is_file()):
                    errors.append(f"missing NBT template {location} in {path.relative_to(DATA)}[{index}]")
                elif target is not None and target.is_file():
                    templates.add(target)
                    template_owners.setdefault(target, set()).add(
                        pool_id_by_path[path]
                    )
                elif isinstance(location, str) and location.startswith("minecraft:"):
                    pools_with_external_templates.add(pool_id_by_path[path])
                    if vanilla_resources is not None:
                        location_path = location.split(":", 1)[1]
                        vanilla_path = f"data/minecraft/structure/{location_path}.nbt"
                        if vanilla_path not in vanilla_resources:
                            errors.append(
                                f"missing vanilla NBT template {location} in {path.relative_to(DATA)}[{index}]"
                            )
            processors = element.get("processors")
            if isinstance(processors, str) and processors.startswith("regnum:") and processors not in processor_ids:
                errors.append(f"missing processor list {processors} in {path.relative_to(DATA)}[{index}]")
            if element.get("element_type") == "minecraft:feature_pool_element":
                feature = element.get("feature", "")
                if isinstance(feature, str) and feature.startswith("regnum:"):
                    target = referenced_path(feature, "worldgen/configured_feature", "json")
                    if target is None or not target.is_file():
                        errors.append(f"missing configured feature {feature} in {path.relative_to(DATA)}[{index}]")

    jigsaw_reference_count = [0]
    loot_table_reference_count = [0]
    connectors_by_template: dict[Path, list[dict[str, str]]] = {}
    for template in templates:
        try:
            nbt, _ = tnbt.read(template.read_bytes())
            check_jigsaw_targets(nbt, template, pool_ids, jigsaw_reference_count)
            check_loot_table_refs(nbt, template, loot_table_reference_count)
            connectors: list[dict[str, str]] = []
            collect_jigsaw_connectors(nbt, connectors)
            connectors_by_template[template] = connectors
        except Exception as exc:
            errors.append(f"invalid NBT template {template.relative_to(DATA)}: {exc}")

    names_by_pool: dict[str, set[str]] = {}
    for template, owners in template_owners.items():
        for connector in connectors_by_template.get(template, []):
            name = connector.get("name", "")
            if name:
                for owner in owners:
                    names_by_pool.setdefault(owner, set()).add(name)
    connector_warnings: set[str] = set()
    pool_edges: dict[str, set[str]] = {}
    terminal_sockets: dict[str, int] = {}
    for template, owners in template_owners.items():
        for connector in connectors_by_template.get(template, []):
            target_pool = connector.get("pool", "")
            target_name = connector.get("target", "")
            if target_pool == "minecraft:empty":
                for owner in owners:
                    terminal_sockets[owner] = terminal_sockets.get(owner, 0) + 1
            elif target_pool in pool_ids and target_name not in ("", "minecraft:empty", "minecraft:bottom"):
                for owner in owners:
                    if target_name in names_by_pool.get(target_pool, set()):
                        pool_edges.setdefault(owner, set()).add(target_pool)
            if (target_pool in pool_ids and target_name not in ("", "minecraft:empty", "minecraft:bottom")
                    and target_name not in names_by_pool.get(target_pool, set())):
                connector_warnings.add(
                    f"{template.relative_to(DATA)}: target {target_name} has no matching name in {target_pool}"
                )

    unreachable_roots: list[str] = []
    external_roots = 0
    checked_roots = 0
    static_roots = 0
    for structure_path, start_pool, start_name in structure_start_pools:
        if start_pool not in pool_ids:
            external_roots += 1
            continue
        if start_name and start_name not in names_by_pool.get(start_pool, set()):
            errors.append(f"start_jigsaw_name {start_name} has no connector in start pool {start_pool} for {structure_path.relative_to(DATA)}")
        root_templates = [template for template, owners in template_owners.items() if start_pool in owners]
        if root_templates and all(not connectors_by_template.get(template, []) for template in root_templates):
            static_roots += 1
            continue
        reachable: set[str] = set()
        pending = [start_pool]
        while pending:
            current = pending.pop()
            if current in reachable:
                continue
            reachable.add(current)
            pending.extend(pool_edges.get(current, set()) - reachable)
        if reachable & pools_with_external_templates:
            external_roots += 1
            continue
        checked_roots += 1
        if not any(terminal_sockets.get(pool, 0) for pool in reachable):
            unreachable_roots.append(str(structure_path.relative_to(DATA)))

    print(f"Worldgen references: {len(pool_ids)} pools, {len(processor_ids)} processor lists, {len(placed_feature_ids)} placed features, {len(biome_modifiers)} biome modifiers, {len(structures)} structures ({len(placed_structures)} placed), {structure_set_count} structure sets ({len(structure_set_salts)} unique random_spread salts), {jigsaw_reference_count[0]} NBT jigsaw targets, {loot_table_reference_count[0]} NBT loot-table refs")
    if vanilla_resources is not None:
        print(f"Vanilla 1.21.1 NBT resources checked: {len(vanilla_resources)}")
    if connector_warnings:
        print(f"WARN: {len(connector_warnings)} jigsaw targets have no matching socket")
        for warning in sorted(connector_warnings):
            print(warning)
    print(f"Internal roots: {checked_roots} jigsaw-reachability checked, {static_roots} static one-piece roots, {external_roots} using external template data")
    if unreachable_roots:
        print(f"WARN: {len(unreachable_roots)} internal roots have no reachable terminal socket")
        for root in sorted(unreachable_roots):
            print(root)
    if errors:
        print(f"FAIL: {len(errors)} invalid references")
        for error in errors:
            print(error)
        return 1
    if "--strict-connectors" in sys.argv and (connector_warnings or unreachable_roots):
        print("FAIL: jigsaw connector audit found unmatched sockets")
        return 1
    print("OK: all checked resource references resolve")
    return 0


if __name__ == "__main__":
    sys.exit(main())
