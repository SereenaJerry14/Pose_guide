package com.photopose.app.ui.viewfinder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.photopose.app.data.model.PoseCategory
import com.photopose.app.data.model.TargetPoseTemplate
import com.photopose.app.ui.theme.AccentCyan
import com.photopose.app.ui.theme.AccentNeonGreen
import com.photopose.app.ui.theme.DarkSurface
import com.photopose.app.ui.theme.LightSurface
import com.photopose.app.ui.theme.TextPrimary
import com.photopose.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoseSelectorSheet(
    sheetState: SheetState,
    categories: List<PoseCategory>,
    selectedCategoryId: String,
    poses: List<TargetPoseTemplate>,
    selectedPoseId: String,
    onCategorySelected: (String) -> Unit,
    onPoseSelected: (TargetPoseTemplate) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Professional Pose Library",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            // Category Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryId.isEmpty() || selectedCategoryId == "all",
                        onClick = { onCategorySelected("all") },
                        label = { Text("All Poses") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentNeonGreen,
                            selectedLabelColor = Color.Black,
                            containerColor = LightSurface,
                            labelColor = TextSecondary
                        )
                    )
                }
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategoryId == cat.id,
                        onClick = { onCategorySelected(cat.id) },
                        label = { Text(cat.title) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentNeonGreen,
                            selectedLabelColor = Color.Black,
                            containerColor = LightSurface,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pose Items List
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(poses) { pose ->
                    val isSelected = pose.id == selectedPoseId
                    PoseCard(
                        pose = pose,
                        isSelected = isSelected,
                        onClick = {
                            onPoseSelected(pose)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PoseCard(
    pose: TargetPoseTemplate,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) AccentNeonGreen else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(LightSurface)
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = pose.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Framing badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(DarkSurface)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = pose.framing,
                        fontSize = 11.sp,
                        color = AccentCyan
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = pose.description,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 17.sp
            )
        }

        if (isSelected) {
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentNeonGreen)
                    .padding(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.Black
                )
            }
        }
    }
}
