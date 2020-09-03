#!/bin/bash

find . -type f -name "*.java" | grep 'test' | while read f
do
	echo "Updating: $f"

	sed -i "s|import org.junit.Test;|import org.junit.jupiter.api.Test;|g" "$f"
	sed -i "s|import org.junit.BeforeClass;|import org.junit.jupiter.api.BeforeAll;|g" "$f"
	sed -i "s|import org.junit.Before;|import org.junit.jupiter.api.BeforeEach;|g" "$f"
	sed -i "s|@BeforeClass|#BeforeAll|g" "$f"
	sed -i "s|@Before|@BeforeEach|g" "$f"
	sed -i "s|#BeforeAll|@BeforeAll|g" "$f"
	sed -i "s|import org.junit.AfterClass;|import org.junit.jupiter.api.AfterAll;|g" "$f"
	sed -i "s|import org.junit.After;|import org.junit.jupiter.api.AfterEach;|g" "$f"
	sed -i "s|@AfterClass|#AfterAll|g" "$f"
	sed -i "s|@After|@AfterEach|g" "$f"
	sed -i "s|#AfterAll|@AfterAll|g" "$f"
	sed -i "s|import org.junit.Ignore;|import org.junit.jupiter.api.Disabled;|g" "$f"
	sed -i "s|@Ignore|@Disabled|g" "$f"
	sed -i "s|import static org.junit.Assert.assertThat;|import static org.hamcrest.MatcherAssert.assertThat;|g" "$f"
	sed -i "s|import org.junit.Assert;|import static org.junit.jupiter.api.Assertions.*;|g" "$f"
	sed -i "s| Assert.assert| assert|g" "$f"
	sed -i "s|import org.junit.Assume;|import static org.junit.jupiter.api.Assumptions.*;|g" "$f"
	sed -i "s| Assume.assume| assume|g" "$f"
	sed -i "s|org.hamcrest.core.IsCollectionContaining|org.hamcrest.core.IsIterableContaining|g" "$f"
	sed -i "s|IsCollectionContaining|IsIterableContaining|g" "$f"
done

