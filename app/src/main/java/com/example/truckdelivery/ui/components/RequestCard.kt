package com.example.truckdelivery.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.truckdelivery.data.model.DeliveryRequest
import com.example.truckdelivery.data.model.RequestStatus
import com.example.truckdelivery.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestCard(
    request: DeliveryRequest,
    onClick: () -> Unit,
    onAccept: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Header with time and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormatter.format(Date(request.createdAt)),
                    style = MaterialTheme.typography.labelMedium
                )
                StatusChip(status = request.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Product details
            Text(
                text = request.productType,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = "Quantity: ${request.quantity}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Distance and ETA (In a real app, calculate these based on actual route)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "~2.5 km away",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "ETA: 10 min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Accept button for pending requests
            if (request.status == RequestStatus.PENDING) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onAccept,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Accept Request")
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    status: RequestStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, contentColor) = when (status) {
        RequestStatus.PENDING -> PendingColor to MaterialTheme.colorScheme.onPrimary
        RequestStatus.ACCEPTED -> InProgressColor to MaterialTheme.colorScheme.onPrimary
        RequestStatus.IN_PROGRESS -> InProgressColor to MaterialTheme.colorScheme.onPrimary
        RequestStatus.COMPLETED -> CompletedColor to MaterialTheme.colorScheme.onPrimary
        RequestStatus.CANCELLED -> CancelledColor to MaterialTheme.colorScheme.onPrimary
    }

    Surface(
        modifier = modifier,
        color = backgroundColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = status.toString()
                .replace("_", " ")
                .lowercase()
                .capitalize(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
    }
}
