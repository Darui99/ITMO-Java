package ru.ifmo.rain.kurbatov.student;

import info.kgeorgiy.java.advanced.student.AdvancedStudentGroupQuery;
import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;

import java.util.*;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class StudentDB implements AdvancedStudentGroupQuery {

    private static final Comparator<Student> CMP_BY_NAME = Comparator.comparing(Student::getLastName).
            thenComparing(Student::getFirstName).thenComparing(Student::compareTo);

    private static String studentFullName(Student x) {
        return x == null ? "" : x.getFirstName() + " " + x.getLastName();
    }

    private <T extends Collection<String>> T getColByFunction(List<Student> students,
                                                              Function<Student, String> function, Supplier<T> collection) {
        return students.stream().map(function).collect(Collectors.toCollection(collection));
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return getColByFunction(students, Student::getFirstName, ArrayList::new);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return getColByFunction(students, Student::getLastName, ArrayList::new);
    }

    @Override
    public List<String> getGroups(List<Student> students) {
        return getColByFunction(students, Student::getGroup, ArrayList::new);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return getColByFunction(students, StudentDB::studentFullName, ArrayList::new);
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return getColByFunction(students, Student::getFirstName, TreeSet::new);
    }

    @Override
    public String getMinStudentFirstName(List<Student> students) {
        return students.stream().min(Student::compareTo).map(Student::getFirstName).orElse("");
    }

    private List<Student> sortedStudents(Collection<Student> students, Comparator<Student> cmp) {
        return students.stream().sorted(cmp).collect(Collectors.toList());
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortedStudents(students, Student::compareTo);
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortedStudents(students, CMP_BY_NAME);
    }

    private Stream<Student> getStreamFilteredByFunc(Collection<Student> students, Predicate<Student> predicate) {
        return students.stream().filter(predicate).sorted(CMP_BY_NAME);
    }

    private Predicate<Student> getPredicate(Function<Student, String> getter, String s) {
        return x -> getter.apply(x).equals(s);
    }

    private List<Student> getListFilteredByField(Collection<Student> students, Function<Student, String> getter, String s) {
        return getStreamFilteredByFunc(students, getPredicate(getter, s)).collect(Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return getListFilteredByField(students, Student::getFirstName, name);
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return getListFilteredByField(students, Student::getLastName, name);
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return getListFilteredByField(students, Student::getGroup, group);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return getStreamFilteredByFunc(students, getPredicate(Student::getGroup, group))
                .collect(Collectors.toMap
                        (Student::getLastName, Student::getFirstName, BinaryOperator.minBy(String::compareTo)));
    }

    private List<Group> splitStudentsByGroup(Collection<Student> students) {
        return students.stream()
                .collect(Collectors.groupingBy(Student::getGroup, TreeMap::new, Collectors.toList()))
                .entrySet().stream().map(x -> new Group(x.getKey(), x.getValue())).collect(Collectors.toList());
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return splitStudentsByGroup(sortStudentsByName(students));
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return splitStudentsByGroup(sortStudentsById(students));
    }


    private Comparator<Group> buildCmp(Function<Group, Long> f) {
        return Comparator.comparing(f).thenComparing(Group::getName, Collections.reverseOrder(String::compareTo));
    }

    private String getMaxByCmp(Collection<Student> students, Comparator<Group> cmp) {
        return splitStudentsByGroup(students).stream().max(cmp).map(Group::getName).orElse("");
    }

    @Override
    public String getLargestGroup(Collection<Student> students) {
        return getMaxByCmp(students, buildCmp(x -> (long) x.getStudents().size()));
    }

    private Map<String, Long> mapByFNCnt(Collection<Student> students) {
        return splitStudentsByGroup(students).stream()
                .collect(Collectors.toMap(Group::getName, x ->
                        x.getStudents().stream().map(Student::getFirstName).distinct().count()));
    }

    private Function<Group, Long> getFuncByMap(Map<String, Long> map) {
        return x -> map.getOrDefault(x.getName(), -1L);
    }

    @Override
    public String getLargestGroupFirstName(Collection<Student> students) {
        return getMaxByCmp(students, buildCmp(getFuncByMap(mapByFNCnt(students))));
    }

    private Map<String, Long> mapCntGroupByFullName(Collection<Student> students) {
        return students.stream().collect(Collectors.groupingBy(StudentDB::studentFullName,
                Collectors.mapping(Student::getGroup,
                        Collectors.collectingAndThen(Collectors.toSet(), x -> (long) x.size()))));
    }

    private String getMostPopularNameByMap(Collection<Student> students, Map<String, Long> map) {
        return students.stream().max(Comparator.comparing
                ((Student x) -> map.getOrDefault(studentFullName(x), -1L))
                .thenComparing(StudentDB::studentFullName)).map(StudentDB::studentFullName).orElse("");
    }

    @Override
    public String getMostPopularName(Collection<Student> students) {
        return getMostPopularNameByMap(students, mapCntGroupByFullName(students));
    }

    private Integer maxInd(int[] indices) {
        return IntStream.of(indices).max().orElse(-1);
    }

    private List<String> getByInd(int[] indices, Collection<Student> students, Function<Student, String> f) {
        return IntStream.of(indices)
                .mapToObj(listByCol(students, maxInd(indices) + 1)::get)
                .map(f).collect(Collectors.toList());
    }

    private List<Student> listByCol(Collection<Student> students, Integer upperBound) {
        return students.stream().limit(upperBound).collect(Collectors.toList());
    }

    @Override
    public List<String> getFirstNames(Collection<Student> students, int[] indices) {
        return getByInd(indices, students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(Collection<Student> students, int[] indices) {
        return getByInd(indices, students, Student::getLastName);
    }

    @Override
    public List<String> getGroups(Collection<Student> students, int[] indices) {
        return getByInd(indices, students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(Collection<Student> students, int[] indices) {
        return getByInd(indices, students, StudentDB::studentFullName);
    }
}
