package com.vnsnord.liqflow.transfer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response payload describing a transfer order together with its line items.
 *
 * @param id                 the order identifier
 * @param orderNumber        the unique business order number
 * @param sourceLocationId   the identifier of the source location
 * @param sourceLocationCode the code of the source location
 * @param targetLocationId   the identifier of the target location
 * @param targetLocationCode the code of the target location
 * @param status             the current lifecycle status
 * @param createdAt          the creation timestamp
 * @param items              the line items of the order
 */
public record TransferOrderDetailResponse(UUID id,
                                          String orderNumber,
                                          UUID sourceLocationId,
                                          String sourceLocationCode,
                                          UUID targetLocationId,
                                          String targetLocationCode,
                                          TransferOrderStatus status,
                                          Instant createdAt,
                                          List<TransferOrderItemResponse> items)
{
}