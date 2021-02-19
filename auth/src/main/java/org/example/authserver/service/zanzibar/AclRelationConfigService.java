package org.example.authserver.service.zanzibar;

import authserver.acl.*;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.service.CacheService;
import org.springframework.stereotype.Service;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AclRelationConfigService {

    private static final Map<Tuple2<String, String>, ConcurrentSkipListSet<FlatRelationTree>> relations = new ConcurrentHashMap<>();

    private final AclRelationConfigRepository repository;
    private final CacheService cacheService;

    public AclRelationConfigService(AclRelationConfigRepository repository, CacheService cacheService) {
        this.repository = repository;
        this.cacheService = cacheService;
    }

    public void save(AclRelationConfig config) {
        repository.save(config);
    }

    public Set<String> nestedRelations(String namespace, String object, String relation) {
        Tuple2<String, String> key = Tuples.of(namespace, object);

        ConcurrentSkipListSet<FlatRelationTree> flatTreeList = relations.get(key);
        if (flatTreeList == null) {
            flatTreeList = relations.get(Tuples.of(namespace, "*"));
        }

        return nestedRelations(relation, flatTreeList);
    }

    public Set<String> nestedRelations(String relation, ConcurrentSkipListSet<FlatRelationTree> flatTreeList) {
        Set<String> result = new HashSet<>();
        result.add(relation);

        if (flatTreeList == null) return result;

        // find current
        FlatRelationTree current = null;
        for (FlatRelationTree flatTree : flatTreeList) {
            if (flatTree.getRelation().equals(relation)) {
                current = flatTree;
                break;
            }
        }
        if (current == null) return result;

        // find nested
        for (FlatRelationTree flatTree : flatTreeList) {
            if (flatTree.getLeft() > current.getLeft() && flatTree.getRight() < current.getRight()) {
                result.add(flatTree.getRelation());
            }
        }

        return result;
    }

    public Set<String> rootRelations(String namespace, String object, String relation) {
        Set<String> result = new HashSet<>();
        result.add(relation);

        Tuple2<String, String> key = Tuples.of(namespace, object);

        ConcurrentSkipListSet<FlatRelationTree> flatTreeList = relations.get(key);
        if (flatTreeList == null) {
            flatTreeList = relations.get(Tuples.of(namespace, "*"));
        }
        if (flatTreeList == null) return result;

        // find current
        FlatRelationTree current = null;
        for (FlatRelationTree flatTree : flatTreeList) {
            if (flatTree.getRelation().equals(relation)) {
                current = flatTree;
                break;
            }
        }
        if (current == null) return result;

        final int currentLevel = current.getLevel();

        ConcurrentSkipListSet<FlatRelationTree> copyFlatTreeList = new ConcurrentSkipListSet<>(flatTreeList);
        copyFlatTreeList.removeIf(f -> f.getLevel() > currentLevel);

        for (FlatRelationTree record : copyFlatTreeList) {
            if (record.equals(current)) continue;
            Set<String> nested = nestedRelations(relation, copyFlatTreeList);
            if (nested.contains(relation)) {
                result.add(record.getRelation());
            }
        }

        return result;
    }

    public void update() {
        Map<Tuple2<String, String>, ConcurrentSkipListSet<FlatRelationTree>> map = getRelationMap();
        relations.keySet().removeIf(key -> !relations.containsKey(key));
        relations.putAll(map);
    }

    public AclRelationConfig getConfig(String key){
        return cacheService.getConfigs().get(key);
    }

    public AclRelation getConfigRelation(String key, String relation) {
        AclRelationConfig config = getConfig(key);
        if (config == null) return null;
        for (AclRelation rel : config.getRelations()){
            if (relation.equals(rel.getRelation())){
                return rel;
            }
        }
        return null;
    }


    public List<FlatRelation> getFlatRelationListFromConfigs() {
        return getFlatRelationListFromConfigs(new HashSet<>(cacheService.getConfigs().values()));
    }

    private List<FlatRelation> getFlatRelationListFromConfigs(Set<AclRelationConfig> configs) {
        List<FlatRelation> relationList = new ArrayList<>();
        for (AclRelationConfig config : configs) {
            for (AclRelation relation : config.getRelations()) {
                relationList.add(new FlatRelation(config.getNamespace(), relation.getRelation(), null));
                if (relation.getParents() != null) {
                    for (AclRelationParent parent : relation.getParents()) {
                        relationList.add(new FlatRelation(config.getNamespace(), relation.getRelation(), parent.getRelation()));
                        if (parent.getParents() != null) {
                            getConfigNestedRelations(config, parent.getParents(), relationList, parent.getRelation());
                        }
                    }
                }
            }
        }

        return relationList;
    }

    private Set<String> getConfigNestedRelations(AclRelationConfig config, Set<AclRelationParent> parents, List<FlatRelation> relationList, String parentRelation) {
        if (parents == null) return new HashSet<>();
        Set<String> result = new HashSet<>();
        for (AclRelationParent parent : parents) {
            String p = String.format("%s#%s", config.getNamespace(), parent.getRelation());
            result.add(p);

            relationList.add(new FlatRelation(config.getNamespace(), parentRelation, parent.getRelation()));

            result.addAll(getConfigNestedRelations(config, parent.getParents(), relationList, parent.getRelation()));
        }

        return result;
    }

    private Map<Tuple2<String, String>, ConcurrentSkipListSet<FlatRelationTree>> getRelationMap() {
        List<FlatRelation> relationList = getFlatRelationListFromConfigs();
        return getRelationMap(relationList);
    }

    private Map<Tuple2<String, String>, ConcurrentSkipListSet<FlatRelationTree>> getRelationMap(List<FlatRelation> relationList) {
        Map<Tuple2<String, String>, ConcurrentSkipListSet<FlatRelationTree>> result = new HashMap<>();

        // namespace tuple {namespace, object} <-> list
        Map<Tuple2<String, String>, List<FlatRelation>> groupedFlatRelations = relationList.stream()
                .collect(Collectors.groupingBy(m -> Tuples.of(m.getNamespace(), m.getObject())));

        for (Map.Entry<Tuple2<String, String>, List<FlatRelation>> entry : groupedFlatRelations.entrySet()) {
            Tuple2<String, String> key = entry.getKey();
            List<FlatRelation> flatRelations = entry.getValue();
            TreeNode<String> root = flat2tree(flatRelations);
            result.put(key, tree2flatRelationTree(root, key));
        }

        return result;
    }

    public Map<Tuple2<String, String>, Set<String>> getRelationSetFromAcls(Set<Acl> acls) {
        Map<Tuple2<String, String>, Set<String>> relations = new HashMap<>();
        for (Acl acl : acls) {
            Tuple2<String, String> ns = Tuples.of(acl.getNamespace(), acl.getObject());
            Set<String> r = relations.getOrDefault(ns, new HashSet<>());
            r.add(acl.getRelation());
            relations.put(ns, r);
            if (acl.hasUserset()) {
                Tuple2<String, String> ns1 = Tuples.of(acl.getUsersetNamespace(), acl.getUsersetObject());
                Set<String> r1 = relations.getOrDefault(ns, new HashSet<>());
                r1.add(acl.getUsersetRelation());
                relations.put(ns1, r1);
            }
        }
        return relations;
    }


    private ConcurrentSkipListSet<FlatRelationTree> tree2flatRelationTree(TreeNode<String> root, Tuple2<String, String> namespace) {
        ConcurrentSkipListSet<FlatRelationTree> result = new ConcurrentSkipListSet<>(Comparator.comparingLong(FlatRelationTree::getLevel));
        for (TreeNode<String> node : root.getElementsIndex()) {
            if (node.equals(root)) continue; // ignore root
            result.add(
                    new FlatRelationTree(namespace.getT1(), namespace.getT2(), node.getData(), node.getLeft(), node.getRight(), node.getLevel())
            );
        }
        return result;
    }

    private TreeNode<String> flat2tree(List<FlatRelation> flatRelations) {
        TreeNode<String> root = new TreeNode<>("root");

        // relation <-> set
        Map<String, Set<FlatRelation>> groupedByRelation = flatRelations.stream()
                .collect(Collectors.groupingBy(FlatRelation::getRelation, Collectors.toSet()));

        Set<FlatRelation> clearedRelations = new HashSet<>();
        for (Map.Entry<String, Set<FlatRelation>> entry : groupedByRelation.entrySet()) {
            boolean existsNonNullParent = isExistsRelationWhereParentIsNotNull(entry.getValue());
            if (existsNonNullParent) {
                entry.getValue().removeIf(r -> r.getParent() == null);
            }
            clearedRelations.addAll(entry.getValue());
        }

        Map<String, TreeNode<String>> nodes = new HashMap<>();

        Queue<FlatRelation> q = new LinkedList<>(clearedRelations);
        while (!q.isEmpty()) {
            FlatRelation relation = q.poll();
            if (relation.getParent() == null) {
                TreeNode<String> node = root.addChild(relation.getRelation()); // add to root
                nodes.put(relation.getRelation(), node);
                continue;
            }

            TreeNode<String> parentNode = nodes.get(relation.getParent());
            if (parentNode == null) {
                q.add(relation);
                continue;
            }

            TreeNode<String> node = parentNode.addChild(relation.getRelation()); // add to parent
            nodes.put(relation.getRelation(), node);
        }

        recalculateLeftRight(root);

        return root;
    }

    private void recalculateLeftRight(TreeNode<String> root) {
        processNode(root, 0);
    }

    private Tuple2<Integer, Integer> processNode(TreeNode<String> parent, int initialId) {
        int left = initialId + 1;
        int right = initialId + 1;
        Iterator<TreeNode<String>> it = parent.getChildren().iterator();
        while (it.hasNext()) {
            TreeNode<String> child = it.next();
            child.setLeft(left);
            Tuple2<Integer, Integer> tuple = processNode(child, left);
            right = tuple.getT2();
            child.setRight(right);

            if (it.hasNext()) {
                left = right + 1;
            }
        }
        return Tuples.of(left, right + 1);
    }

    private boolean isExistsRelationWhereParentIsNotNull(Set<FlatRelation> relations) {
        for (FlatRelation relation : relations) {
            if (relation.getParent() != null) return true;
        }

        return false;
    }

}
