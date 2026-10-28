package com.springapi.common.dto;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * Stable JSON shape for paginated results. Use this instead of returning Spring's {@link Page}
 * directly, whose JSON structure is not guaranteed to stay the same between versions.
 */
public record PageResponse<T>(
		List<T> content,
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean last) {

	public static <T> PageResponse<T> from(Page<T> page) {
		return new PageResponse<>(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isLast());
	}

}
