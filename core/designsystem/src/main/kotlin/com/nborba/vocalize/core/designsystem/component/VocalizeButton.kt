package com.nborba.vocalize.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.nborba.vocalize.core.designsystem.icon.VocalizeIcons
import com.nborba.vocalize.core.designsystem.preview.ThemePreviews
import com.nborba.vocalize.core.designsystem.theme.VocalizeTheme
import com.nborba.vocalize.core.designsystem.theme.spacing

/**
 * Default button.
 */
@Composable
fun VocalizeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        contentPadding =
            PaddingValues(
                horizontal = MaterialTheme.spacing.medium,
                vertical = MaterialTheme.spacing.small,
            ),
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
        }
        content()
    }
}

/**
 * Default button - Convenience overload.
 */
@Composable
fun VocalizeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    VocalizeButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null, // Decorative icon inside button
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
        }
        Text(text = text)
    }
}

/**
 * Secondary Outlined Vocalize Button.
 */
@Composable
fun VocalizeOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        contentPadding =
            PaddingValues(
                horizontal = MaterialTheme.spacing.medium,
                vertical = MaterialTheme.spacing.small,
            ),
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
        }
        content()
    }
}

/**
 * Secondary Outlined Vocalize Button - Convenience overload.
 */
@Composable
fun VocalizeOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    VocalizeOutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null, // Decorative icon inside button
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
        }
        Text(text = text)
    }
}

/**
 * Ghost/Text Vocalize Button.
 */
@Composable
fun VocalizeTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        content = content,
    )
}

/**
 * Ghost/Text Vocalize Button - Convenience Overload.
 */
@Composable
fun VocalizeTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    VocalizeTextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        Text(text = text)
    }
}

/**
 * Floating Action Button (FAB).
 */
@Composable
fun VocalizeFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = FloatingActionButtonDefaults.shape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    content: @Composable () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        content = content,
    )
}

/**
 * Floating Action Button - Convenience overload for Icon-only FAB.
 */
@Composable
fun VocalizeFloatingActionButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    shape: Shape = FloatingActionButtonDefaults.shape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
) {
    VocalizeFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
        )
    }
}

/**
 * Extended Floating Action Button - Convenience overload for Icon + Text FAB.
 */
@Composable
fun VocalizeExtendedFloatingActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    expanded: Boolean = true,
    shape: Shape = FloatingActionButtonDefaults.shape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        expanded = expanded || icon == null,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        icon = {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                )
            }
        },
        text = { Text(text = text) },
    )
}

/**
 * Circular Action Button (e.g. large record or play button).
 */
@Composable
fun VocalizeCircularButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = contentColorFor(containerColor),
    content: @Composable () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
            ),
        contentPadding = PaddingValues(MaterialTheme.spacing.medium),
        content = { content() },
    )
}

/**
 * Circular Action Button - Convenience overload for Icon.
 */
@Composable
fun VocalizeCircularButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = contentColorFor(containerColor),
) {
    VocalizeCircularButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        containerColor = containerColor,
        contentColor = contentColor,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
        )
    }
}

/**
 * Small Circular Action Button (e.g. secondary action controls like stop, cancel, or bookmark).
 */
@Composable
fun VocalizeFilledIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = contentColorFor(containerColor),
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = CircleShape,
        colors =
            IconButtonDefaults.filledIconButtonColors(
                containerColor = containerColor,
                contentColor = contentColor,
            ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
        )
    }
}

@ThemePreviews
@Composable
private fun VocalizeButtonsPreview() {
    VocalizeTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Primary Filled Buttons
                VocalizeButton(
                    text = "Primary Button",
                    onClick = {},
                )
                VocalizeButton(
                    text = "Primary with Icon",
                    leadingIcon = VocalizeIcons.Check,
                    onClick = {},
                )
                VocalizeButton(
                    text = "Primary Disabled",
                    enabled = false,
                    onClick = {},
                )

                // Secondary Outlined Buttons
                VocalizeOutlinedButton(
                    text = "Outlined Button",
                    onClick = {},
                )
                VocalizeOutlinedButton(
                    text = "Outlined with Icon",
                    leadingIcon = VocalizeIcons.Settings,
                    onClick = {},
                )
                VocalizeOutlinedButton(
                    text = "Outlined Disabled",
                    enabled = false,
                    onClick = {},
                )

                // Text / Ghost Buttons
                VocalizeTextButton(
                    text = "Text Button",
                    onClick = {},
                )
                VocalizeTextButton(
                    text = "Text Disabled",
                    enabled = false,
                    onClick = {},
                )

                // Floating Action Buttons
                VocalizeFloatingActionButton(
                    icon = VocalizeIcons.Record,
                    onClick = {},
                )
                VocalizeExtendedFloatingActionButton(
                    text = "Record",
                    icon = VocalizeIcons.Record,
                    onClick = {},
                )

                // Circular Buttons
                VocalizeCircularButton(
                    icon = VocalizeIcons.Record,
                    onClick = {},
                )
                VocalizeCircularButton(
                    icon = VocalizeIcons.Pause,
                    onClick = {},
                )
                VocalizeFilledIconButton(
                    icon = VocalizeIcons.Stop,
                    onClick = {},
                )
            }
        }
    }
}
